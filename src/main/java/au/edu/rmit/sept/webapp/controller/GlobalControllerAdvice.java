package au.edu.rmit.sept.webapp.controller; // Adjust package as necessary

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${google.maps.api.key}") 
    private String GOOGLE_API_KEY;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("GOOGLE_API_KEY", GOOGLE_API_KEY);
    }
}