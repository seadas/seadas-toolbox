/*
 * Native main executable of SeaDAS.app (Contents/MacOS/SeaDAS), and of "Install SeaDAS.app".
 *
 * It runs a script with /bin/sh and the same arguments: by default Contents/app/bin/seadas.
 * SCRIPT (relative to Contents/MacOS) and NAME (for error messages) can be set when compiling.
 * A compiled executable is used instead of a script because a signed script keeps its
 * signature in extended attributes, which are lost when the app is copied by tools that
 * don't keep them, and the app then looks damaged to Gatekeeper.
 *
 * Build (build-mac-app.sh does this on a Mac):
 *   cc -O2 -arch arm64 -mmacosx-version-min=12.0 -o SeaDAS launcher.c
 * and for the installer (build-mac-installer.sh):
 *   cc ... -DNAME='"Install SeaDAS"' -DSCRIPT='"../Resources/start-installer.sh"' -o "Install SeaDAS" launcher.c
 */
#include <mach-o/dyld.h>
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#ifndef NAME
#define NAME "SeaDAS"
#endif
#ifndef SCRIPT
#define SCRIPT "../app/bin/seadas"
#endif

int main(int argc, char *argv[]) {
    char exe[PATH_MAX];
    char real[PATH_MAX];
    char script[PATH_MAX];
    uint32_t size = sizeof(exe);

    if (_NSGetExecutablePath(exe, &size) != 0 || realpath(exe, real) == NULL) {
        fprintf(stderr, NAME ": can't find the application folder\n");
        return 1;
    }
    /* real = .../<name>.app/Contents/MacOS/<name> -> .../<name>.app/Contents/MacOS */
    char *slash = strrchr(real, '/');
    if (slash == NULL) {
        fprintf(stderr, NAME ": unexpected path %s\n", real);
        return 1;
    }
    *slash = '\0';
    if (snprintf(script, sizeof(script), "%s/" SCRIPT, real) >= (int) sizeof(script)) {
        fprintf(stderr, NAME ": path too long\n");
        return 1;
    }

    char **args = calloc((size_t) argc + 2, sizeof(char *));
    if (args == NULL) {
        return 1;
    }
    int n = 0;
    args[n++] = "/bin/sh";
    args[n++] = script;
    for (int i = 1; i < argc; i++) {
        /* Older macOS versions pass a process serial number when started from Finder. */
        if (strncmp(argv[i], "-psn_", 5) == 0) {
            continue;
        }
        args[n++] = argv[i];
    }
    args[n] = NULL;

    execv("/bin/sh", args);
    perror(NAME ": can't run /bin/sh");
    return 1;
}
