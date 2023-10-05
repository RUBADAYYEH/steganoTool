import java.io.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBufferByte;

// Simple java script allowing hiding or revealing data in image files using Least Significant Bit algorithm.
public class Steganographer {

    private static int bytesForTextLengthData = 4; //size of an int .
    private static int bitsInByte = 8;

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args.length == 1) {
                if (args[0].equals("--help")) {
                    System.out.println("");
                    System.out.println("-- STEGANOGRAPHER --");
                    System.out.println("");
                    System.out.println("Hide or reveal data in images!");
                    System.out.println("");
                    System.out.println("For encode mode provide two arguments as specified below:");
                    System.out.println("java Steganographer <path_to_container_image> <path_to_message_text_file>");
                    System.out.println("");
                    System.out.println("For decode mode provide only one argument as specified below:");
                    System.out.println("java Steganographer <path_to_image_with_hidden_message>");
                    System.out.println("");
                    return;
                } else {
                    decode(args[0]);
                    return;
                }
            } else if (args.length == 2) {
                encode(args[0], args[1]);
                return;
            }
        }
        System.out.println("Wrong input. Use '--help' option for more information.");
    }


    // Encode

    private static void encode(String imagePath, String textPath) {
        BufferedImage originalImage = getImageFromPath(imagePath);
        BufferedImage imageInUserSpace = getImageInUserSpace(originalImage);
        String text = getTextFromTextFile(textPath);

        byte imageInBytes[] = getBytesFromImage(imageInUserSpace);
        byte textInBytes[] = text.getBytes(); // array of bytes used in a strting
        byte textLengthInBytes[] = getBytesFromInt(textInBytes.length); //hexadecimal representation of the length in 4 bytes
        try {
            //the encode method calls encodeImage twice because it needs to embed both the length of the hidden message and the actual message content into the image. Separating the length and content allows for easier extraction during decoding
            // , as the decoder can first extract the length and then use it to determine where the content is located in the image.
            encodeImage(imageInBytes, textLengthInBytes,  0); // to embed the message  length
            encodeImage(imageInBytes, textInBytes, bytesForTextLengthData*bitsInByte);//to embed the actual message
        }
        catch (Exception exception) {
            System.out.println("Couldn't hide text in image. Error: " + exception);
            return;
        }

        String fileName = imagePath;
        int position = fileName.lastIndexOf(".");
        if (position > 0) {
            fileName = fileName.substring(0, position);
        }

        String finalFileName = fileName + "_with_hidden_message.png";
        System.out.println("Successfully encoded text in: " + finalFileName);
        saveImageToPath(imageInUserSpace, new File(finalFileName),"png");
        return;
    }
///distrubution of bits ?????
    private static byte[] encodeImage(byte[] image, byte[] addition, int offset) {
        if (addition.length + offset > image.length) {
            throw new IllegalArgumentException("Image file is not long enough to store provided text");
        }
        for (int i=0; i<addition.length; i++) {
            int additionByte = addition[i]; //  (loops)takes every byte of  the text .
            for (int bit=bitsInByte-1; bit>=0; --bit, offset++) {  // loops through every bit of a bite .
                int b = (additionByte >>> bit) & 0x1;                 //0x1: This is a hexadecimal representation of the binary value 00000001.
                                                                     // It's used to mask the result of the right shift operation to
                //                                                    ensure that only the least significant bit remains.
                image[offset] = (byte)((image[offset] & 0xFE) | b); //clear the lest significant bit in the image byte and or it with b .
            }
        }
        return image;
    }


    // Decode

    private static String decode(String imagePath) {
        byte[] decodedHiddenText;
        try {
            BufferedImage imageFromPath = getImageFromPath(imagePath);
            BufferedImage imageInUserSpace = getImageInUserSpace(imageFromPath);
            byte imageInBytes[] = getBytesFromImage(imageInUserSpace);
            decodedHiddenText = decodeImage(imageInBytes);
            String hiddenText = new String(decodedHiddenText);
            String outputFileName = "hidden_text.txt";
            saveTextToPath(hiddenText, new File(outputFileName));
            System.out.println("Successfully extracted text to: " + outputFileName);
            return hiddenText;
        } catch (Exception exception) {
            System.out.println("No hidden message. Error: " + exception);
            return "";
        }
    }

    private static byte[] decodeImage(byte[] image) {
        int length = 0;
        int offset  = bytesForTextLengthData*bitsInByte;

        for (int i=0; i<offset; i++) {    // to calculate the length of the hidden message .
            length = (length << 1) | (image[i] & 0x1); //left shift and extract the lsb.
        }

        byte[] result = new byte[length];

        for (int b=0; b<result.length; b++ ) {
            for (int i=0; i<bitsInByte; i++, offset++) {
                result[b] = (byte)((result[b] << 1) | (image[offset] & 0x1));
            }
        }
        return result;
    }


    // File I/O methods

    private static void saveImageToPath(BufferedImage image, File file, String extension) {
        try {
            file.delete();
            ImageIO.write(image, extension, file);
        } catch (Exception exception) {
            System.out.println("Image file could not be saved. Error: " + exception);
        }
    }

    private static void saveTextToPath(String text, File file) {
        try {
            if (file.exists() == false) {
                file.createNewFile( );
            }
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(text);
            bufferedWriter.close();
        } catch (Exception exception) {
            System.out.println("Couldn't write text to file: " + exception);
        }
    }

    private static BufferedImage getImageFromPath(String path) {
        BufferedImage image	= null;
        File file = new File(path);
        try {
            image = ImageIO.read(file);
        } catch (Exception exception) {
            System.out.println("Input image cannot be read. Error: " + exception);
        }
        return image;
    }

    private static String getTextFromTextFile(String textFile) {
        String text = "";
        try {
            Scanner scanner = new Scanner( new File(textFile) );
            text = scanner.useDelimiter("\\A").next();   // scanner.useDelimiter("\\A") sets the delimiter for the scanner to \\A,
                                                                // which is a regular expression pattern that matches the beginning of input.
                                     // Essentially, it instructs the scanner to read the entire contents of the file as a single token.

                    scanner.close();
        } catch (Exception exception) {
            System.out.println("Couldn't read text from file. Error: " + exception);
        }
        return text;
    }


    // Helpers

    //creating a copy of image . in type model of rgb to make sure it doesnt affect the original image .

    private static BufferedImage getImageInUserSpace(BufferedImage image) {
        BufferedImage imageInUserSpace  = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = imageInUserSpace.createGraphics(); //The Graphics2D class provides a way to draw and manipulate graphics within the image.
        graphics.drawRenderedImage(image, null);//graphics.drawRenderedImage(image, null): This line draws the content of the original input image onto the imageInUserSpace.
        // Essentially, it copies the pixels from the original image to the new image.
        graphics.dispose();
        return imageInUserSpace;
    }

    private static byte[] getBytesFromImage(BufferedImage image) {
        WritableRaster raster = image.getRaster(); //The raster represents the rectangular array of pixels that make up the image.
        DataBufferByte buffer = (DataBufferByte)raster.getDataBuffer(); // convert the pixels into bytes of rgb .
        return buffer.getData();
    }

    private static byte[] getBytesFromInt(int integer) {
        return ByteBuffer.allocate(bytesForTextLengthData).putInt(integer).array();
    }
}