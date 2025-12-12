package au.edu.rmit.sept.webapp.service;

public interface QrCodeService {
    byte[] generateQrCode(String text, int width, int height);
}