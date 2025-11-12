import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.logging.*;

public class JPG_PDF {

    private static final Logger LOGGER = Logger.getLogger(JPG_PDF.class.getName());

    // A4 size in points (1 point = 1/72 inch)
    private static final float A4_WIDTH_PT = 595f;
    private static final float A4_HEIGHT_PT = 842f;

    // DPI scale for image → PDF mapping
    private static final float TARGET_DPI = 150f; // balance between quality and size

    public static void main(String[] args) {
        String inputPath = "C:\\Users\\GowrakkavariVenkataS\\Downloads\\xx_2.jpg";
        String outputPath = "C:\\Users\\GowrakkavariVenkataS\\Downloads\\JPG_PDF_Converted_5mb.pdf";

        LOGGER.info("Starting image to A4 PDF conversion...");

        try {
            convertToA4Pdf(inputPath, outputPath);
            LOGGER.info("PDF saved successfully at: " + outputPath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Conversion failed: " + e.getMessage(), e);
        }
    }

    public static void convertToA4Pdf(String imagePath, String pdfPath) throws IOException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new FileNotFoundException("Image not found: " + imagePath);
        }

        BufferedImage original = ImageIO.read(imageFile);
        if (original == null) {
            throw new IOException("Invalid or unreadable image file: " + imagePath);
        }

        // Convert A4 page size to pixels for target DPI
        int A4_WIDTH_PX = Math.round(A4_WIDTH_PT / 72f * TARGET_DPI);
        int A4_HEIGHT_PX = Math.round(A4_HEIGHT_PT / 72f * TARGET_DPI);
        //LOGGER.info(String.format("A4 page size: %d x %d px", A4_WIDTH_PX, A4_HEIGHT_PX));

        double scale = Math.min(
            (double) A4_WIDTH_PX / original.getWidth(),
            (double) A4_HEIGHT_PX / original.getHeight()
        );

        int newW = (int) (original.getWidth() * scale);
        int newH = (int) (original.getHeight() * scale);

        // Resize image
        Image tmp = original.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", imageStream);
        byte[] imgData = imageStream.toByteArray();

        try (FileOutputStream fos = new FileOutputStream(pdfPath);
             OutputStreamWriter writer = new OutputStreamWriter(fos, "ISO-8859-1")) {

            writer.write("%PDF-1.4\n");
            writer.write("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
            writer.write("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");

            writer.write(String.format(
                "3 0 obj << /Type /Page /Parent 2 0 R /Resources << /XObject <</Im0 4 0 R>> >> /MediaBox [0 0 %.2f %.2f] /Contents 5 0 R >> endobj\n",
                A4_WIDTH_PT, A4_HEIGHT_PT));

            writer.write(String.format(
                "4 0 obj << /Type /XObject /Subtype /Image /Width %d /Height %d /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length %d >> stream\n",
                newW, newH, imgData.length));
            writer.flush();
            fos.write(imgData);
            fos.flush();
            writer.write("\nendstream\nendobj\n");

            double offsetX = (A4_WIDTH_PT - (newW * 72.0 / TARGET_DPI)) / 2.0;
            double offsetY = (A4_HEIGHT_PT - (newH * 72.0 / TARGET_DPI)) / 2.0;

            String content = String.format(
                "q\n%.2f 0 0 %.2f %.2f %.2f cm /Im0 Do\nQ\n",
                (double) newW * 72.0 / TARGET_DPI,
                (double) newH * 72.0 / TARGET_DPI,
                offsetX, offsetY
            );

            writer.write(String.format("5 0 obj << /Length %d >> stream\n%s\nendstream\nendobj\n", content.length(), content));
            writer.write("xref\n0 6\n0000000000 65535 f \n");
            writer.write("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n0\n%%EOF");
        }

        LOGGER.info("PDF written successfully (" +
                new File(pdfPath).length() / 1024 + " KB)");
    }
}
