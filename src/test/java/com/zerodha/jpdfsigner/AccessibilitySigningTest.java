package com.zerodha.jpdfsigner;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfBoolean;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for PDF/UA accessibility survival through signing.
 *
 * <p>
 * Signing rewrites the whole document (PdfStamper in non-append mode), and it
 * attaches a widget annotation to the signature page. Both of those can break
 * PDF/UA-1 conformance, so these tests pin down:
 * <ul>
 * <li>{@code /Tabs /S} exists on every page that ends up with annotations,
 * including the signature page (ISO 14289-1, 7.18.3)</li>
 * <li>{@code /ViewerPreferences /DisplayDocTitle true} is present, and existing
 * viewer preferences such as {@code /Direction} are not clobbered (7.1)</li>
 * <li>tag structure, document metadata and the screen-reader permission
 * survive</li>
 * <li>the output signature is still cryptographically valid</li>
 * </ul>
 */
class AccessibilitySigningTest {

    private static final String PASSWORD = "testpassword";

    private static PrivateKey privateKey;
    private static Certificate[] chain;

    @TempDir
    Path tempDir;

    private OpenPdfSigner signer;

    @BeforeAll
    static void generateSigningCredentials() throws Exception {
        // config.ini and *.pfx are gitignored, so the test generates its own
        // throwaway self-signed credentials instead of relying on repo files.
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Principal subject = new X500Principal("CN=jpdfsigner test, O=Zerodha, C=IN");
        long now = System.currentTimeMillis();
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.ONE,
                new Date(now - 86_400_000L),
                new Date(now + 365L * 86_400_000L),
                subject,
                keyPair.getPublic());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(
                        new JcaContentSignerBuilder("SHA256WithRSA")
                                .setProvider("BC")
                                .build(keyPair.getPrivate())));

        privateKey = keyPair.getPrivate();
        chain = new Certificate[] { cert };
    }

    @BeforeEach
    void setUp() {
        signer = new OpenPdfSigner();
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * Builds a fixture shaped like the real Typst {@code --pdf-standard ua-1}
     * output: tagged, {@code /Direction} but no {@code /DisplayDocTitle} in
     * ViewerPreferences, and {@code /Tabs} absent everywhere.
     *
     * <p>
     * Page 1 has no annotations (it becomes the signature page), page 2 has no
     * annotations at all, page 3 carries a link annotation.
     */
    private Path buildTaggedPdf(String name) throws Exception {
        return buildPdf(name, true);
    }

    private Path buildUntaggedPdf(String name) throws Exception {
        return buildPdf(name, false);
    }

    private Path buildPdf(String name, boolean tagged) throws Exception {
        Path path = tempDir.resolve(name);
        Document doc = new Document(PageSize.A4);
        try (FileOutputStream out = new FileOutputStream(path.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            if (tagged) {
                writer.setTagged();
                writer.setPdfVersion(PdfWriter.VERSION_1_7);
                writer.addViewerPreference(PdfName.DIRECTION, PdfName.L2R);
            }
            doc.addTitle("Contract note fixture");
            doc.addAuthor("Zerodha");
            doc.addSubject("Fixture subject");
            doc.addKeywords("contract note, fixture");
            doc.open();
            doc.add(new Paragraph("page one"));
            doc.newPage();
            doc.add(new Paragraph("page two"));
            doc.newPage();
            doc.add(new Paragraph("page three"));
            writer.addAnnotation(PdfAnnotation.createLink(
                    writer,
                    new Rectangle(50, 50, 150, 70),
                    PdfAnnotation.HIGHLIGHT_NONE,
                    new PdfAction("https://example.com")));
            doc.close();
        }
        return path;
    }

    private Path sign(Path src, String password) throws Exception {
        Path dest = tempDir.resolve("signed-" + src.getFileName());
        SignParams params = new SignParams();
        params.setSrc(src.toString());
        params.setDest(dest.toString());
        params.setPassword(password);
        params.setReason("Regulatory");
        params.setContact("Zerodha Broking Limited");
        params.setLocation("Bangalore");
        params.setKey(privateKey);
        params.setChain(chain);
        params.setRect(new Rectangle(0, 609, 278, 550));
        params.setPage(1);
        params.setFont(new Font(Font.HELVETICA, 9));
        signer.sign(params);
        return dest;
    }

    private PdfReader read(Path pdf, String password) throws IOException {
        return password == null || password.isEmpty()
                ? new PdfReader(pdf.toString())
                : new PdfReader(pdf.toString(), password.getBytes());
    }

    // ------------------------------------------------------------------- tests

    @Test
    void sign_taggedPdf_addsTabsToSignaturePage() throws Exception {
        Path src = buildTaggedPdf("tabs-sig-page.pdf");

        // Precondition: the source has no /Tabs on the page we are about to sign.
        try (AutoCloseablePdf before = new AutoCloseablePdf(read(src, null))) {
            assertNull(before.reader.getPageN(1).get(PdfName.TABS),
                    "fixture should start without /Tabs on page 1");
            assertNull(before.reader.getPageN(1).get(PdfName.ANNOTS),
                    "fixture page 1 should start without annotations");
        }

        Path signed = sign(src, PASSWORD);

        try (AutoCloseablePdf after = new AutoCloseablePdf(read(signed, PASSWORD))) {
            PdfDictionary page1 = after.reader.getPageN(1);
            assertNotNull(page1.get(PdfName.ANNOTS),
                    "signing should have attached the signature widget to page 1");
            assertEquals(PdfName.S, page1.get(PdfName.TABS),
                    "page gaining an annotation from signing must get /Tabs /S");
        }
    }

    @Test
    void sign_taggedPdf_addsTabsToAlreadyAnnotatedPageAndLeavesOthersAlone() throws Exception {
        Path signed = sign(buildTaggedPdf("tabs-other-pages.pdf"), PASSWORD);

        try (AutoCloseablePdf after = new AutoCloseablePdf(read(signed, PASSWORD))) {
            assertEquals(PdfName.S, after.reader.getPageN(3).get(PdfName.TABS),
                    "page 3 has a link annotation, so it needs /Tabs /S");
            assertNull(after.reader.getPageN(2).get(PdfName.TABS),
                    "page 2 has no annotations, so /Tabs must not be invented");
        }
    }

    @Test
    void sign_taggedPdf_setsDisplayDocTitleWithoutDroppingOtherViewerPreferences() throws Exception {
        Path signed = sign(buildTaggedPdf("viewer-prefs.pdf"), PASSWORD);

        try (AutoCloseablePdf after = new AutoCloseablePdf(read(signed, PASSWORD))) {
            PdfDictionary prefs = after.reader.getCatalog().getAsDict(PdfName.VIEWERPREFERENCES);
            assertNotNull(prefs, "ViewerPreferences must survive signing");
            assertEquals(PdfBoolean.PDFTRUE.toString(),
                    String.valueOf(prefs.get(PdfName.DISPLAYDOCTITLE)),
                    "PDF/UA requires /DisplayDocTitle true");
            // Guards against using PdfStamper.addViewerPreference(), which rebuilds the
            // dictionary from scratch and would silently drop /Direction.
            assertEquals(PdfName.L2R, prefs.get(PdfName.DIRECTION),
                    "pre-existing viewer preferences must not be clobbered");
        }
    }

    @Test
    void sign_taggedPdf_preservesTagStructureAndMetadata() throws Exception {
        Path signed = sign(buildTaggedPdf("structure.pdf"), PASSWORD);

        try (AutoCloseablePdf after = new AutoCloseablePdf(read(signed, PASSWORD))) {
            PdfDictionary catalog = after.reader.getCatalog();
            PdfDictionary markInfo = catalog.getAsDict(PdfName.MARKINFO);
            assertNotNull(markInfo, "/MarkInfo must survive signing");
            assertTrue(markInfo.getAsBoolean(PdfName.MARKED).booleanValue(),
                    "document must still be marked as tagged");
            assertNotNull(catalog.get(PdfName.STRUCTTREEROOT),
                    "/StructTreeRoot must survive signing");
            assertEquals("Contract note fixture", after.reader.getInfo().get("Title"));
            assertEquals("Zerodha", after.reader.getInfo().get("Author"));
            assertEquals("Fixture subject", after.reader.getInfo().get("Subject"));
            assertEquals("contract note, fixture", after.reader.getInfo().get("Keywords"));
        }
    }

    @Test
    void sign_withPassword_encryptsAndAllowsScreenReaders() throws Exception {
        Path signed = sign(buildTaggedPdf("permissions.pdf"), PASSWORD);

        try (AutoCloseablePdf after = new AutoCloseablePdf(read(signed, PASSWORD))) {
            assertTrue(after.reader.isEncrypted(), "password protection must still apply");
            int permissions = after.reader.getPermissions();
            assertTrue((permissions & PdfWriter.ALLOW_SCREENREADERS) != 0,
                    "screen reader extraction must remain permitted");
            assertTrue((permissions & PdfWriter.ALLOW_PRINTING) != 0,
                    "printing must remain permitted");
        }
    }

    @Test
    void sign_untaggedPdf_leavesAccessibilityEntriesUntouched() throws Exception {
        Path signed = sign(buildUntaggedPdf("untagged.pdf"), "");

        try (AutoCloseablePdf after = new AutoCloseablePdf(read(signed, null))) {
            assertNull(after.reader.getCatalog().get(PdfName.VIEWERPREFERENCES),
                    "untagged documents must not gain ViewerPreferences");
            for (int page = 1; page <= after.reader.getNumberOfPages(); page++) {
                assertNull(after.reader.getPageN(page).get(PdfName.TABS),
                        "untagged documents must not gain /Tabs (page " + page + ")");
            }
        }
    }

    @Test
    void sign_taggedPdf_producesCryptographicallyValidSignature() throws Exception {
        Path signed = sign(buildTaggedPdf("crypto.pdf"), PASSWORD);
        byte[] fileBytes = Files.readAllBytes(signed);

        try (AutoCloseablePdf after = new AutoCloseablePdf(read(signed, PASSWORD))) {
            AcroFields fields = after.reader.getAcroFields();
            assertEquals(1, fields.getSignatureNames().size(), "expected exactly one signature");
            String name = fields.getSignatureNames().get(0);
            assertTrue(fields.signatureCoversWholeDocument(name),
                    "signature must cover the whole document");

            PdfDictionary sigDict = fields.getSignatureDictionary(name);
            assertEquals("Regulatory", sigDict.getAsString(PdfName.REASON).toString());

            // /adbe.pkcs7.sha1 encapsulates the SHA-1 digest of the signed byte range,
            // so validity means: the CMS signer verifies AND that digest matches the
            // bytes actually on disk.
            PdfArray byteRange = sigDict.getAsArray(PdfName.BYTERANGE);
            ByteArrayOutputStream covered = new ByteArrayOutputStream();
            for (int i = 0; i < byteRange.size(); i += 2) {
                covered.write(fileBytes,
                        byteRange.getAsNumber(i).intValue(),
                        byteRange.getAsNumber(i + 1).intValue());
            }
            byte[] expectedDigest = MessageDigest.getInstance("SHA-1").digest(covered.toByteArray());

            byte[] pkcs7 = sigDict.getAsString(PdfName.CONTENTS).getOriginalBytes();
            CMSSignedData signedData = new CMSSignedData(pkcs7);
            assertArrayEquals(expectedDigest, (byte[]) signedData.getSignedContent().getContent(),
                    "signed byte-range digest must match the bytes on disk");

            for (SignerInformation signerInfo : signedData.getSignerInfos().getSigners()) {
                @SuppressWarnings("unchecked")
                Collection<X509CertificateHolder> matches = (Collection<X509CertificateHolder>) signedData
                        .getCertificates().getMatches(signerInfo.getSID());
                X509Certificate cert = new JcaX509CertificateConverter()
                        .setProvider("BC")
                        .getCertificate(matches.iterator().next());
                assertTrue(signerInfo.verify(new JcaSimpleSignerInfoVerifierBuilder()
                        .setProvider("BC").build(cert)), "CMS signer must verify");
            }
        }
    }

    @Test
    void sign_taggedPdf_keepsPageContentIntact() throws Exception {
        Path src = buildTaggedPdf("content.pdf");
        Path signed = sign(src, PASSWORD);

        try (AutoCloseablePdf before = new AutoCloseablePdf(read(src, null));
                AutoCloseablePdf after = new AutoCloseablePdf(read(signed, PASSWORD))) {
            assertEquals(before.reader.getNumberOfPages(), after.reader.getNumberOfPages());
            // Pages we do not touch must keep their /StructParents mapping into the tag
            // tree, otherwise the structure tree would point at the wrong content.
            for (int page = 1; page <= before.reader.getNumberOfPages(); page++) {
                assertEquals(
                        String.valueOf(before.reader.getPageN(page).get(new PdfName("StructParents"))),
                        String.valueOf(after.reader.getPageN(page).get(new PdfName("StructParents"))),
                        "/StructParents must be stable for page " + page);
            }
        }
    }

    /** Small helper so tests can close readers via try-with-resources. */
    private static final class AutoCloseablePdf implements AutoCloseable {
        private final PdfReader reader;

        AutoCloseablePdf(PdfReader reader) {
            this.reader = reader;
        }

        @Override
        public void close() {
            reader.close();
        }
    }
}
