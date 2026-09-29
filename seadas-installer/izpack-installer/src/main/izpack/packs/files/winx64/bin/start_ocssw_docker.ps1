<#
Starts the SeaDAS OCSSW server in Docker (the seadas/ocssw-run image) for
OCSSW location "docker".  SeaDAS runs this script itself when the server is
not answering; it can also be run by hand:

    powershell -ExecutionPolicy Bypass -File start_ocssw_docker.ps1

  1. checks that Docker Desktop is installed and running (starts it if needed),
  2. creates the OCSSW and shared directories,
  3. copies %USERPROFILE%\.netrc (or _netrc, Earthdata Login) into the shared directory,
  4. pulls the image if it is not there yet,
  5. starts the container, recreating it if the image, directories or ports changed,
  6. waits until the server answers.

Windows counterpart of bin/start_ocssw_docker on Linux and macOS; keep the two in step.
Works in Windows PowerShell 5.1 and PowerShell 7.
#>
param(
    [string]$Image = "seadas/ocssw-run:12.0.0",
    [string]$Name = "seadas-ocssw",
    [string]$OcsswDir = (Join-Path $env:USERPROFILE "ocssw-docker"),
    [string]$SharedDir = (Join-Path $env:USERPROFILE "seadasClientServerShared"),
    [string]$Netrc = "",
    [int]$Port = 6400,
    [int]$InputPort = 6402,
    [int]$ErrorPort = 6403,
    [int]$Timeout = 180
)

# Exit codes, also read by SeaDAS.
$E_NO_DOCKER = 2; $E_DAEMON = 3; $E_PULL = 4; $E_START = 5; $E_TIMEOUT = 6; $E_DIRS = 7

function Step([string]$Message) { Write-Output "==> $Message" }
function Fail([int]$Code, [string]$Message) {
    [Console]::Error.WriteLine("ERROR: $Message")
    exit $Code
}
function Docker-Ok { & docker @args *> $null; return ($LASTEXITCODE -eq 0) }
function Docker-Text { return ((& docker @args 2> $null) | Out-String).Trim() }

# --- 1. Docker ---------------------------------------------------------------
Step "Checking Docker"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    $bin = Join-Path $env:ProgramFiles "Docker\Docker\resources\bin"
    if (Test-Path (Join-Path $bin "docker.exe")) {
        $env:Path = "$env:Path;$bin"
    } else {
        Fail $E_NO_DOCKER "Docker is not installed. Install Docker Desktop from https://docs.docker.com/desktop/setup/install/windows-install/"
    }
}

if (-not (Docker-Ok info)) {
    $desktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    if (Test-Path $desktop) {
        Step "Starting Docker Desktop"
        Start-Process -FilePath $desktop
        for ($i = 0; $i -lt 60; $i++) {
            Start-Sleep -Seconds 2
            if (Docker-Ok info) { break }
        }
    }
    if (-not (Docker-Ok info)) {
        Fail $E_DAEMON "Docker is not running. Start Docker Desktop and try again."
    }
}

# --- 2. Directories ----------------------------------------------------------
Step "Creating $OcsswDir and $SharedDir"
try {
    New-Item -ItemType Directory -Force -Path $OcsswDir, $SharedDir -ErrorAction Stop | Out-Null
} catch {
    Fail $E_DIRS "Cannot create $OcsswDir or ${SharedDir}: $($_.Exception.Message)"
}
$OcsswDir = (Resolve-Path $OcsswDir).Path
$SharedDir = (Resolve-Path $SharedDir).Path

# --- 3. Earthdata Login ------------------------------------------------------
if (-not $Netrc) {
    $Netrc = Join-Path $env:USERPROFILE ".netrc"
    if (-not (Test-Path $Netrc)) { $Netrc = Join-Path $env:USERPROFILE "_netrc" }
}
$netrcChanged = $false
$sharedNetrc = Join-Path $SharedDir ".netrc"
if (Test-Path $Netrc -PathType Leaf) {
    $same = (Test-Path $sharedNetrc) -and
            ((Get-FileHash $Netrc).Hash -eq (Get-FileHash $sharedNetrc).Hash)
    if (-not $same) {
        Step "Copying $Netrc into $SharedDir"
        try {
            Copy-Item -Force -Path $Netrc -Destination $sharedNetrc -ErrorAction Stop
        } catch {
            Fail $E_DIRS "Cannot copy $Netrc to ${SharedDir}: $($_.Exception.Message)"
        }
        $netrcChanged = $true
    }
} else {
    [Console]::Error.WriteLine("WARNING: no .netrc or _netrc in $env:USERPROFILE. OCSSW needs Earthdata Login credentials to download ancillary data; see https://urs.earthdata.nasa.gov/")
}

# --- 4. Image ----------------------------------------------------------------
if (-not (Docker-Ok image inspect $Image)) {
    Step "Downloading $Image (first time only, this can take several minutes)"
    & docker pull --platform linux/amd64 $Image
    if ($LASTEXITCODE -ne 0) { Fail $E_PULL "Could not download $Image" }
}

# --- 5. Container ------------------------------------------------------------
# Everything that is fixed when the container is created goes into a label, so
# a change in any of it (new SeaDAS version, other directories or ports) recreates it.
$config = "image=$Image ocssw=$OcsswDir shared=$SharedDir ports=$Port,$InputPort,$ErrorPort hostnet=0"
$label = "gov.nasa.gsfc.seadas.ocssw.config"

if (Docker-Ok container inspect $Name) {
    # Read the labels as JSON: Windows PowerShell 5.1 would strip the quotes an
    # "index" template needs for the dotted label name.
    $labels = Docker-Text container inspect -f "{{json .Config.Labels}}" $Name | ConvertFrom-Json
    $current = if ($labels) { $labels.$label } else { $null }
    if ($current -ne $config) {
        Step "Settings changed, recreating container $Name"
        if (-not (Docker-Ok rm -f $Name)) { Fail $E_START "Cannot remove the old container $Name" }
    }
}

if (Docker-Ok container inspect $Name) {
    if ((Docker-Text container inspect -f "{{.State.Running}}" $Name) -eq "true") {
        if ($netrcChanged) {
            # The server reads .netrc from the shared directory only when it starts.
            Step "Restarting container $Name for the new .netrc"
            if (-not (Docker-Ok restart $Name)) { Fail $E_START "Cannot restart container $Name" }
        } else {
            Step "Container $Name is already running"
        }
    } else {
        Step "Starting container $Name"
        if (-not (Docker-Ok start $Name)) { Fail $E_START "Cannot start container $Name" }
    }
} else {
    Step "Creating container $Name"
    & docker run -d --name $Name --platform linux/amd64 `
        --label "$label=$config" `
        -p "${Port}:6400" -p "${InputPort}:6402" -p "${ErrorPort}:6403" `
        -v "${SharedDir}:/root/seadasClientServerShared" `
        -v "${OcsswDir}:/root/ocssw" `
        $Image | Out-Null
    if ($LASTEXITCODE -ne 0) { Fail $E_START "Cannot start container $Name (is port $Port already in use?)" }
}

# --- 6. Wait for the server --------------------------------------------------
# Any HTTP answer, even an error status, means the server is up.
function Test-Server {
    try {
        $request = [System.Net.WebRequest]::Create("http://localhost:$Port/ocsswws/")
        $request.Timeout = 2000
        $request.GetResponse().Close()
        return $true
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) { $_.Exception.Response.Close(); return $true }
        return $false
    } catch {
        return $false
    }
}

Step "Waiting for the OCSSW server on port $Port"
for ($i = 0; $i -lt $Timeout; $i++) {
    if (Test-Server) {
        Step "OCSSW server is up"
        exit 0
    }
    if ((Docker-Text container inspect -f "{{.State.Running}}" $Name) -ne "true") {
        & docker logs --tail 20 $Name
        Fail $E_START "Container $Name stopped; its last log lines are above"
    }
    Start-Sleep -Seconds 1
}
& docker logs --tail 20 $Name
Fail $E_TIMEOUT "The OCSSW server did not answer on port $Port within $Timeout seconds"
