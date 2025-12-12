package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.service.QrCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QrCodeController.class)
public class QrCodeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrCodeService qrCodeService;

    @Test
    void getQrCode() throws Exception {
        //checks if it sucessfully returns qr code image
        Long eventId = 1L;
        Long userId = 1L;
        byte[] qrCodeBytes = new byte[]{1, 2, 3};
        String expectedUrl = "http://localhost:8080/checkin/" + eventId + "/" + userId;

        when(qrCodeService.generateQrCode(expectedUrl, 250, 250)).thenReturn(qrCodeBytes);

        mockMvc.perform(get("/qrcode/{eventId}/{userId}", eventId, userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(qrCodeBytes));
    }
}