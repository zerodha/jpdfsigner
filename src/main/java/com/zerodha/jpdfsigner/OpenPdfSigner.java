package com.zerodha.jpdfsigner;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import io.undertow.Undertow;

import static io.undertow.Handlers.path;

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
    void sign(String src, String dest, String password,
            PrivateKey key, Certificate[] chain,
            String reason, String contact, String location, Rectangle rect,
            Font font, int page) throws DocumentException, IOException {

        // Creating the reader and the stamper
        PdfReader reader;
        reader = new PdfReader(src);

        FileOutputStream os;
        os = new FileOutputStream(dest);

        PdfStamper stp = PdfStamper.createSignature(reader, os, '\0', null);

        // Is there a password?
        if (password.length() > 0) {
            byte[] p = password.getBytes();

            // PdfWriter.DO_NOT_ENCRYPT_METADATA somehow disables password protection.
            stp.setEncryption(p, p, PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128);
        }

        PdfSignatureAppearance sap = stp.getSignatureAppearance();

        sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
        sap.setReason(reason);
        sap.setContact(contact);
        sap.setLocation(location);
        sap.setVisibleSignature(rect, page);
        sap.setLayer2Font(font);
        sap.setAcro6Layers(true);
        stp.close();
    }

    // Get the list of PDF files to convert from a CSV file.
    // Each line in the file should be in the format:
    // in_file.pdf|out_file.pdf|password
    // where password is optional. The function returns an arraylist of lists in the
    // format:
    // [[infile, outfile, password], [infile, outfile, password] ...]
    private ArrayList<String[]> getListFromFile(String infile) throws IOException {
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
    private ArrayList<String[]> getListFromDirectory(String srcDir, String targetDir) {
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

                list.add(new String[] { srcPath, targetDir + "/" + targetName, password });
            }
        }

        return list;
    }

    public static void main(String[] args)
            throws DocumentException, IOException, GeneralSecurityException {

        // Check if the config file exists.
        File configFile = new File("config.ini");

        if (!configFile.exists()) {
            System.out.println("config.ini file not found. Please create a config.ini file.");
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
    public static void startServer(Properties config) throws DocumentException, IOException, GeneralSecurityException {
        // Initialize the app.
        OpenPdfSigner app = new OpenPdfSigner();

        // Properties from the config file
        Font font = new Font(Font.HELVETICA, 9);
        font.setColor(16, 181, 60);
        font.setStyle("bold");

        String reason = config.getProperty("reason"),
                contact = config.getProperty("contact"),
                location = config.getProperty("location");

        float[] coords = new float[] {
                Float.parseFloat(config.getProperty("x1")),
                Float.parseFloat(config.getProperty("y1")),
                Float.parseFloat(config.getProperty("x2")),
                Float.parseFloat(config.getProperty("y2"))
        };
        Rectangle rect = new Rectangle(coords[0], coords[1], coords[2], coords[3]);

        int page = Integer.parseInt(config.getProperty("page"));

        // Initialize OpenPDF crypto.
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(new FileInputStream(config.getProperty("keyfile")), config.getProperty("password").toCharArray());

        String alias = ks.aliases().nextElement();
        PrivateKey key = (PrivateKey) ks.getKey(alias, config.getProperty("password").toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        ExecutorService executor = Executors.newCachedThreadPool();

        int port = Integer.parseInt(config.getProperty("server_port", "8090"));
        String host = config.getProperty("server_host", "localhost");
        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(path().addExactPath("/sign", httpExchange -> {
                    SigningRequest request = new SigningRequest(key, chain, reason, contact, location, rect, app,
                            font, page, executor);
                    request.handleRequestWithMeta(httpExchange);
                })).build();

        server.start();
    }

    // Start CLI.
    public static void startCLI(String[] args, Properties config)
            throws DocumentException, IOException, GeneralSecurityException {
        if (args.length < 1) {
            System.out.println("Contract notes PDF signer for Zerodha\n\n1) PdfSigner file_list.txt");
            System.out.println(
                    "The file list should have one entry per line and each entry should be: input.pdf output.pdf");
            System.out.println("2) PdfSigner input_dir output_dir");
            System.out.println("3) Starts a HTTP server if server = true is set in config.");
            System.exit(0);
        }

        // Initialize the app.
        OpenPdfSigner app = new OpenPdfSigner();

        // Properties from the config file
        Font font = new Font(Font.HELVETICA, 9);
        font.setColor(16, 181, 60);
        font.setStyle("bold");

        String reason = config.getProperty("reason"),
                contact = config.getProperty("contact"),
                location = config.getProperty("location");

        float[] coords = new float[] {
                Float.parseFloat(config.getProperty("x1")),
                Float.parseFloat(config.getProperty("y1")),
                Float.parseFloat(config.getProperty("x2")),
                Float.parseFloat(config.getProperty("y2"))
        };
        Rectangle rect = new Rectangle(coords[0], coords[1], coords[2], coords[3]);

        int page = Integer.parseInt(config.getProperty("page"));

        // Initialize OpenPDF crypto.
        KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(new FileInputStream(config.getProperty("keyfile")), config.getProperty("password").toCharArray());

        String alias = ks.aliases().nextElement();
        PrivateKey key = (PrivateKey) ks.getKey(alias, config.getProperty("password").toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        // Load file list from an input list or from an input directory
        ArrayList<String[]> flist;
        if (args.length == 2) {
            if (args[0].equals(args[1])) {
                System.out.println("Can't read and write from the same directory");
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
            app.sign(fl[0], fl[1], fl[2],
                    key,
                    chain,
                    reason,
                    contact,
                    location,
                    rect,
                    font,
                    page);

            if ((i + 1) % 100 == 0) {
                System.out.println(i);
            }
        }

        System.out.println("Done");
    }
}
