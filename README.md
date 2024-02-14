<a href="https://zerodha.tech"><img src="https://zerodha.tech/static/images/github-badge.svg" align="right" /></a>

# jpdfsigner

A simple Java CLI and HTTP server for using OpenPDF to digitally sign and password protect PDF files. The digital signature is obtained from a PFX file.

Tested with OpenJDK/JRE v17+.

## Building

### Pre-requisites:

Needs OpenJDK 17+ and Maven 3.6+ installed.

For building, run `make`. It creates a `jpdfsigner-1.0-SNAPSHOT.jar` file in the `./target` directory.

## Usage

### Configuration

See `config.sample.ini` for the configuration.

### HTTP Server

For running the server, run `java -jar ./target/jpdfsigner-1.0-SNAPSHOT.jar`. This starts the HTTP server is the `server` is true in `config.ini`.

#### API

```
POST `/sign`
```

Request:

```json
{
  "input_file": "path_to_input.pdf",
  "output_file": "path_to_output.pdf",
  "password": "password",
  "reason": "reason for signing",
  "contact": "contact",
  "location": "ACME Corp, India"
}
```

If `location`, `contact`, and `reason` are not provided, the default values from the `config.ini` are used.

### CLI

The program can be used a CLI tool as well.

#### Convert files described in a CSV list

- Prepare a `|` separated CSV file in the following format `path_to_input.pdf|path_to_output.pdf|password`. Password is optional.
- `java -jar jpdfsigner-1.0-SNAPSHOT.jar filelist.csv`

#### Convert files in a directory

- `java -jar jpdfsigner-1.0-SNAPSHOT.jar /path/to/input/directory /path/to/output/directory`
- If the filenames contain `_`, the first part will be used as a password and the last part as the output filename.

#### Bulk signing

The server is multi threaded but the CLI is single threaded, so to achive higher throughput for bulk signing a large number of files using the CLI, orchestrate running multiple concurrent processes of jpdfsigner per core on the system.
