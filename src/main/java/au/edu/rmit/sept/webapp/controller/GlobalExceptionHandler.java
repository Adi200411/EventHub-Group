package au.edu.rmit.sept.webapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler to provide better error messages for file upload issues
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle file upload size exceeded exceptions
     * This catches the HTTP 413 errors that occur when files exceed Spring Boot's configured limits
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(
            MaxUploadSizeExceededException exc,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        
        logger.warn("File upload size exceeded. Max allowed size: {}, Request size: {}", 
                    exc.getMaxUploadSize(), 
                    request.getContentLength());

        String errorMessage = "File size exceeds the maximum limit of 25MB. Please choose a smaller image file.";
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        
        // Try to redirect back to the referring page, or fall back to gallery
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/gallery/event/")) {
            return "redirect:" + referer.substring(referer.indexOf("/gallery/event/"));
        }
        
        return "redirect:/gallery";
    }
}