package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.service.QrCodeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class QrCodeController {

    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/qrcode/{eventId}/{userId}")
    public ResponseEntity<byte[]> getQrCode(@PathVariable("eventId") Long eventId, @PathVariable("userId") Long userId) {
        String qrCodeText = "http://localhost:8080/checkin/" + eventId + "/" + userId;
        byte[] qrCode = qrCodeService.generateQrCode(qrCodeText, 250, 250);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCode);
    }
}