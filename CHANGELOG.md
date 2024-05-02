# v2.0.1 (2024-05-02)

### Bugfixes/Maintenance:
* Updated component package versions
  * bioformats 7.2.0
  * bioformats2raw 0.9.2
* Fixed a bug when trying to open a deleted image
* Custom directories which do not exist will now be created when saving task settings

### Known issues:
* The underlying bioformats package is currently unable to read some formats on ARM-based MacOS systems.
* raw2ometiff execution cannot be reliably interrupted until it finishes the initial scan of a .zarr file.

# v2.0.0 (2024-02-16)

### Features:
* Redesigned user interface, modernised layout and design.
* NGFF-Converter now directly interfaces with bioformats2raw/raw2ometiff instead of generating a CLI call.
* Modularised conversion into "Jobs" (internally called "Workflows") comprised of a sequence of "Tasks".
* Added virtual "Output" task to control destination files.
* Added full settings configuration panels for all tasks.
* Added progress bars per-task during execution.
* Revised configuration workflow. Settings are now initialised to user-defined defaults. Jobs can be configured individually or in bulk.
* Job execution now uses a proper queue, so jobs can be started/stopped individually.
* Execution logs are now stored and delivered per-job.
* Added configuration options for working directory and log directory (if logging to file).


### Bugfixes/Maintenance:
* Updated component package versions
  * bioformats 7.1.0
  * bioformats2raw 0.9.1
  * raw2ometiff 0.7.0
* Updated documentation

### Known issues:
* The underlying bioformats package is currently unable to read some formats on ARM-based MacOS systems.
* raw2ometiff execution cannot be reliably interrupted until it finishes the initial scan of a .zarr file.

# v1.1.5 (2023-06-26)

### Bugfixes/Maintenance:
* Update component package versions (#41)
  * bioformats 6.13.0
  * bioformats2raw 0.7.0
  * raw2ometiff 0.5.0
* Expand documentation (#48)

# v1.1.4 (2023-01-10)

### Bugfixes/Maintenance:
* Remove year from Copyright in About window (#37)

# v1.1.3 (2023-01-09)

### Bugfixes/Maintenance:
* Temporarily drop support for M1 Macs, breaks x86_64

# v1.1.2 (2023-01-07)

### Bugfixes/Maintenance:
* Fix app image for code signing
* Add limited support for M1 Macs

# v1.1.1 (2023-01-05)

### Bugfixes/Maintenance:
* Switch to app image to aid code signing workflow (#36).

# v1.1.0 (2022-12-15)

### Features:
* NGFF-Converter now generates OME-NGFF files according to [version 0.4](https://ngff.openmicroscopy.org/0.4/) of the format specification.
* See the [bioformats2raw changelog](https://github.com/glencoesoftware/bioformats2raw/releases/tag/v0.6.0) for details of new features.

### Bugfixes/Maintenance:
* bioformats2raw, raw2ometiff, bioformats and logback versions have been updated (#27, #30).
* MIRAX .mrxs files can now be converted (#31).
* Fixed an issue which could prevent running conversions from stopping upon exit (#33).
* Improvements to the build/packaging system. Application filesize should now be much smaller (#29, #24).
* UI tweaks (#28)

# v1.0.3 (2022-09-16)

### Features:
* Initial public release
