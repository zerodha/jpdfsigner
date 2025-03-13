<a href="https://zerodha.tech"><img src="https://zerodha.tech/static/images/github-badge.svg" align="right" /></a>

# jpdfsigner

A simple Java CLI and HTTP server for using OpenPDF to digitally sign and password protect PDF files. The digital signature is obtained from a PFX file. Now with AWS S3 integration for reading and writing PDFs directly from S3 buckets.

Tested with OpenJDK/JRE v17+ and v21+.

## Building

### Pre-requisites:

Needs OpenJDK 17+/21+ and Maven 3.6+ installed.

For building, run `make` or `mvn package`. It creates a `jpdfsigner-1.0-SNAPSHOT.jar` file in the `./target` directory.

## Testing

To run the unit tests:

```bash
mvn test
```

The test framework uses JUnit 5 and Mockito for unit testing. Tests are located in the `src/test/java` directory.

## Usage

### Configuration

See `config.sample.ini` for the configuration.

#### AWS S3 Integration

jpdfsigner can now read from and write to AWS S3 buckets. To use this feature:

1. Set `s3_enabled=true` in your config.ini
2. Configure the `s3_region` parameter with your AWS region (e.g., "us-east-1")
3. Ensure AWS credentials are properly configured through one of the following methods:
   - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
   - ~/.aws/credentials file
   - EC2 instance profiles
   - Container credentials

When using the API or CLI, you can specify S3 paths using the format: `s3://bucket-name/path/to/file.pdf`

### HTTP Server

For running the server, run `java -jar ./target/jpdfsigner-1.0-SNAPSHOT.jar`. This starts the HTTP server if the `server` is true in `config.ini`.

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

For S3 integration, use S3 paths:

```json
{
  "input_file": "s3://bucket-name/path/to/input.pdf",
  "output_file": "s3://bucket-name/path/to/output.pdf",
  "password": "password"
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
