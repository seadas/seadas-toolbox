# SeaDAS Panoply Tools

Panoply‑inspired features for SeaDAS/SNAP.  
This module currently auto‑attaches a **Panoply‑style metadata tree** to any NetCDF/CF product you open, with room to grow into plotting, diagnostics, and group browsing.

## Features

- **Auto metadata attachment** on product open:
    - `Panoply/Global_Attributes`
    - `Panoply/Dimensions`
    - `Panoply/Variables` (type, shape, attributes)
    - `Panoply/Coordinate_Systems` (from NetcdfDataset enhancement)
    - `Panoply/Coverage_Summary` (CF global coverage attrs if present)
- Zero UI clicks required; appears under **Product Explorer → Metadata**.

## Module Layout

seadas-panoply-tools/
└─ src/main/java/gov/nasa/gsfc/seadas/panoply/
├─ StartupHook.java # registers product-open listener (@OnStart)
└─ PanoplyStyleMetadataBuilder.java # builds & attaches the metadata subtree


## Build

This module is part of the SeaDAS toolbox reactor.

1. Ensure the parent `seadas/pom.xml` declares matching versions:
   ```xml
   <properties>
     <snap.version>12.0.1</snap.version>        <!-- or your target SNAP version -->
     <netbeans.version>RELEASE210</netbeans.version>  <!-- example -->
   </properties>
