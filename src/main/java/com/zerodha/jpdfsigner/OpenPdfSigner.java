package com.zerodha.jpdfsigner;

import static io.undertow.Handlers.path;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import io.undertow.Undertow;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenPdfSigner {

    private S3Handler s3Handler;

    /**
     * Set the S3Handler to use for S3 operations
     */
    public void setS3Handler(S3Handler s3Handler) {
        this.s3Handler = s3Handler;
    }

    /**
     * Get the S3Handler instance (primarily for testing)
     */
    S3Handler getS3Handler() {
        return this.s3Handler;
    }

    /**
     * Initialize S3Handler from configuration if S3 is enabled
     * 
     * @param config Properties containing s3_enabled and s3_region
     * @return Initialized S3Handler or null if S3 is disabled
     */
    public static S3Handler initializeS3Handler(Properties config) {
        S3Handler s3Handler = null;
        boolean s3Enabled = Boolean.parseBoolean(config.getProperty("s3_enabled", "false"));

        if (s3Enabled) {
            String s3Region = config.getProperty("s3_region", "us-east-1");
            try {
                s3Handler = new S3Handler(s3Region);

                // If S3Handler was created successfully
                if (s3Handler != null && s3Handler.getS3Client() != null) {
                    System.out.println("S3 support enabled with region: " + s3Region);
                } else {
                    s3Handler = null;
                    System.err.println("Failed to initialize S3Handler with region: " + s3Region);
                    System.err.println("S3 support will be disabled");
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to initialize S3Handler: " + e.getMessage());
                System.err.println("S3 support will be disabled");
            }
        }

        return s3Handler;
    }

    /**
     * Initialize common configuration properties used for PDF signing
     * 
     * @param config Properties containing signature configuration
     * @return SignatureConfig object containing all common signature settings
     * @throws IOException              if there's an error loading files
     * @throws GeneralSecurityException if there's an error loading the keystore
     */
    public static SignatureConfig initializeSignatureConfig(Properties config)
            throws IOException, GeneralSecurityException {
        // Initialize font settings
        Font font = new Font(Font.HELVETICA, 9);
        font.setColor(16, 181, 60);
        font.setStyle("bold");

        // Signature properties
        String reason = config.getProperty("reason");
        String contact = config.getProperty("contact");
        String location = config.getProperty("location");

        // Signature rectangle coordinates
        float[] coords = new float[] {
                Float.parseFloat(config.getProperty("x1")),
                Float.parseFloat(config.getProperty("y1")),
                Float.parseFloat(config.getProperty("x2")),
                Float.parseFloat(config.getProperty("y2")),
        };
        Rectangle rect = new Rectangle(coords[0], coords[1], coords[2], coords[3]);

        // Signature page
        int page = Integer.parseInt(config.getProperty("page"));

        // Initialize certificate and keys
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(
                new FileInputStream(config.getProperty("keyfile")),
                config.getProperty("password").toCharArray());

        String alias = ks.aliases().nextElement();
        PrivateKey key = (PrivateKey) ks.getKey(
                alias,
                config.getProperty("password").toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        // Return all configuration in a single object
        return new SignatureConfig(font, reason, contact, location, rect, page, key, chain);
    }

    void sign(SignParams params) throws DocumentException, IOException {
        PdfReader reader;
        InputStream inputStream = null;
        ByteArrayOutputStream outputBuffer = null;
        FileOutputStream fileOutputStream = null;
        ByteArrayInputStream resultStream = null;

        boolean isInputS3 = S3Handler.isS3Path(params.getSrc());
        boolean isOutputS3 = S3Handler.isS3Path(params.getDest());

        try {
            // Handle input based on whether it's S3 or filesystem
            if (isInputS3) {
                if (s3Handler == null) {
                    throw new IllegalStateException("S3Handler not initialized but S3 path provided for input");
                }
                // Get the file from S3 as an InputStream
                try {
                    inputStream = s3Handler.getInputStreamFromS3(params.getSrc());
                    reader = new PdfReader(inputStream);
                    System.out.println("Reading input from S3: " + params.getSrc());
                } catch (Exception e) {
                    throw new IOException("Failed to read PDF from S3: " + params.getSrc(), e);
                }
            } else {
                // Local file input - use existing flow
                reader = new PdfReader(params.getSrc());
                System.out.println("Reading input from filesystem: " + params.getSrc());
            }

            // Handle output based on whether it's S3 or filesystem
            if (isOutputS3) {
                if (s3Handler == null) {
                    throw new IllegalStateException("S3Handler not initialized but S3 path provided for output");
                }
                // Use ByteArrayOutputStream to hold the signed PDF data
                outputBuffer = new ByteArrayOutputStream();
                PdfStamper stp = PdfStamper.createSignature(reader, outputBuffer, '\0', null);

                // Apply signature
                applySignature(stp, params);

                // Upload the signed PDF to S3
                try {
                    resultStream = new ByteArrayInputStream(outputBuffer.toByteArray());
                    s3Handler.uploadToS3(resultStream, outputBuffer.size(), params.getDest());
                    System.out.println("Uploaded output to S3: " + params.getDest());
                } catch (Exception e) {
                    throw new IOException("Failed to upload signed PDF to S3: " + params.getDest(), e);
                }
            } else {
                // Local file output - use existing flow
                fileOutputStream = new FileOutputStream(params.getDest());
                PdfStamper stp = PdfStamper.createSignature(reader, fileOutputStream, '\0', null);

                // Apply signature
                applySignature(stp, params);
                System.out.println("Wrote output to filesystem: " + params.getDest());
            }
        } finally {
            // Clean up resources
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing input stream: " + e.getMessage());
                }
            }
            if (outputBuffer != null) {
                try {
                    outputBuffer.close();
                } catch (IOException e) {
                    System.err.println("Error closing output buffer: " + e.getMessage());
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing file output stream: " + e.getMessage());
                }
            }
            if (resultStream != null) {
                try {
                    resultStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing result stream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Apply the signature to the PDF stamper
     */
    private void applySignature(PdfStamper stp, SignParams params) throws DocumentException, IOException {
        // Is there a password?
        if (params.getPassword() != null && !params.getPassword().isEmpty()) {
            byte[] p = params.getPassword().getBytes();

            // PdfWriter.DO_NOT_ENCRYPT_METADATA somehow disables password protection.
            stp.setEncryption(
                    p,
                    p,
                    PdfWriter.ALLOW_PRINTING,
                    PdfWriter.ENCRYPTION_AES_128);
        }

        PdfSignatureAppearance sap = stp.getSignatureAppearance();

        sap.setCrypto(
                params.getKey(),
                params.getChain(),
                null,
                PdfSignatureAppearance.WINCER_SIGNED);
        sap.setReason(params.getReason());
        sap.setContact(params.getContact());
        sap.setLocation(params.getLocation());
        sap.setVisibleSignature(params.getRect(), params.getPage(), null);
        sap.setLayer2Font(params.getFont());
        sap.setAcro6Layers(true);
        stp.close();
    }

    // Get the list of PDF files to convert from a CSV file.
    // Each line in the file should be in the format:
    // in_file.pdf|out_file.pdf|password
    // where password is optional. The function returns an arraylist of lists in the
    // format:
    // [[infile, outfile, password], [infile, outfile, password] ...]
    private ArrayList<String[]> getListFromFile(String infile)
            throws IOException {
        ArrayList<String[]> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(infile));

        String line;
        while ((line = br.readLine()) != null) {
            String[] ch = line.trim().split("\\|");
            if (ch.length == 3) {
                list.add(ch);
            } else if (ch.length == 2) {
                list.add(new String[] { ch[0], ch[1], "" });
            }
        }

        br.close();
        return list;
    }

    // Get the list of PDFs to convert from a directory. The filename should be in
    // the format
    // PASSWORD_filename.pdf OR filename.pdf.
    // The function returns an arraylist of lists in the format:
    // [[infile, outfile, password], [infile, outfile, password] ...]
    private ArrayList<String[]> getListFromDirectory(
            String srcDir,
            String targetDir) {
        ArrayList<String[]> list = new ArrayList<>();

        File folder = new File(srcDir);
        File[] files = folder.listFiles();

        assert files != null;
        for (File file : files) {
            String srcPath = file.getPath();
            if (file.isFile() && srcPath.toLowerCase().endsWith(".pdf")) {
                // If the filename has an underscore, the last part is used as the
                // output filename while the rest is used as the password.
                String targetName = file.getName();
                String[] ch = targetName.split("_");
                String password = "";
                if (ch.length > 1) {
                    targetName = ch[ch.length - 1];
                    password = ch[0];
                }

                list.add(
                        new String[] {
                                srcPath,
                                targetDir + "/" + targetName,
                                password,
                        });
            }
        }

        return list;
    }

    public static void main(String[] args)
            throws DocumentException, IOException, GeneralSecurityException {
        // Check if the config file exists.
        File configFile = new File("config.ini");

        if (!configFile.exists()) {
            System.out.println(
                    "config.ini file not found. Please create a config.ini file.");
            System.exit(1);
        }

        // Load the config.
        FileInputStream inp = new FileInputStream("config.ini");
        Properties config = new Properties();
        config.load(inp);

        // Check if we have to run a server.
        boolean runServer = Boolean.parseBoolean(config.getProperty("server"));

        // If the `server` is true, start the HTTP server instead of signing the PDFs.
        if (runServer) {
            System.out.println("Starting server");
            startServer(config);
        } else {
            startCLI(args, config);
        }
    }

    // Start server with the given config.
    public static void startServer(Properties config)
            throws DocumentException, IOException, GeneralSecurityException {
        // Initialize the app.
        OpenPdfSigner app = new OpenPdfSigner();
        S3Handler s3Handler = null;

        try {
            // Initialize S3Handler if enabled
            s3Handler = initializeS3Handler(config);
            if (s3Handler != null) {
                app.setS3Handler(s3Handler);
            }

            // Initialize common signature configuration
            SignatureConfig sigConfig = initializeSignatureConfig(config);

            ExecutorService executor = Executors.newCachedThreadPool();

            int port = Integer.parseInt(config.getProperty("server_port", "8090"));
            String host = config.getProperty("server_host", "localhost");
            Undertow server = Undertow.builder()
                    .addHttpListener(port, host)
                    .setHandler(
                            path()
                                    .addExactPath("/sign", httpExchange -> {
                                        SigningRequest request = new SigningRequest(
                                                sigConfig.getKey(),
                                                sigConfig.getChain(),
                                                sigConfig.getReason(),
                                                sigConfig.getContact(),
                                                sigConfig.getLocation(),
                                                sigConfig.getRect(),
                                                app,
                                                sigConfig.getFont(),
                                                sigConfig.getPage(),
                                                executor);
                                        request.handleRequestWithMeta(httpExchange);
                                    }))
                    .build();

            server.start();
        } finally {
            // This will only execute if there's an exception earlier in the method,
            // since server.start() blocks the thread
            if (s3Handler != null) {
                try {
                    s3Handler.close();
                    System.out.println("S3Handler resources released");
                } catch (Exception e) {
                    System.err.println("Error closing S3Handler: " + e.getMessage());
                }
            }
        }
    }

    // Start CLI.
    public static void startCLI(String[] args, Properties config)
            throws DocumentException, IOException, GeneralSecurityException {
        if (args.length < 1) {
            System.out.println(
                    "Contract notes PDF signer for Zerodha\n\n1) PdfSigner file_list.txt");
            System.out.println(
                    "The file list should have one entry per line and each entry should be: input.pdf output.pdf");
            System.out.println("2) PdfSigner input_dir output_dir");
            System.out.println(
                    "3) Starts a HTTP server if server = true is set in config.");
            System.exit(0);
        }

        // Initialize the app.
        OpenPdfSigner app = new OpenPdfSigner();
        S3Handler s3Handler = null;

        try {
            // Initialize S3Handler if enabled
            s3Handler = initializeS3Handler(config);
            if (s3Handler != null) {
                app.setS3Handler(s3Handler);
            }

            // Initialize common signature configuration
            SignatureConfig sigConfig = initializeSignatureConfig(config);

            // Load file list from an input list or from an input directory
            ArrayList<String[]> flist;
            if (args.length == 2) {
                if (args[0].equals(args[1])) {
                    System.out.println(
                            "Can't read and write from the same directory");
                    System.exit(0);
                }
                flist = app.getListFromDirectory(args[0], args[1]);
            } else {
                flist = app.getListFromFile(args[0]);
            }

            // Sign.
            System.out.println("Signing " + flist.size() + " files");

            // Run through and sign each file
            for (int i = 0; i < flist.size(); i++) {
                String[] fl = flist.get(i);

                SignParams params = new SignParams();
                params.setSrc(fl[0]);
                params.setDest(fl[1]);
                params.setPassword(fl[2]);
                params.setReason(sigConfig.getReason());
                params.setContact(sigConfig.getContact());
                params.setLocation(sigConfig.getLocation());
                params.setKey(sigConfig.getKey());
                params.setChain(sigConfig.getChain());
                params.setRect(sigConfig.getRect());
                params.setFont(sigConfig.getFont());
                params.setPage(sigConfig.getPage());

                app.sign(params);

                if ((i + 1) % 100 == 0) {
                    System.out.println(i);
                }
            }

            System.out.println("Done");
        } finally {
            if (s3Handler != null) {
                try {
                    s3Handler.close();
                    System.out.println("S3Handler resources released");
                } catch (Exception e) {
                    System.err.println("Error closing S3Handler: " + e.getMessage());
                }
            }
        }
    }
}
