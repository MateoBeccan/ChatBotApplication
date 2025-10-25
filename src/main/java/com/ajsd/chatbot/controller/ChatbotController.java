package com.ajsd.chatbot.controller;

import com.ajsd.chatbot.model.ConversationContext;
import com.ajsd.chatbot.service.RuleBasedEngine;
import com.ajsd.chatbot.service.ValidationService;
import com.ajsd.chatbot.service.RateLimitService;
import com.ajsd.chatbot.service.MetricsService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@SessionAttributes("conversationContext")
public class ChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);
    private final RuleBasedEngine ruleBasedEngine;
    private final ValidationService validationService;
    private final RateLimitService rateLimitService;
    private final MetricsService metricsService;

    public ChatbotController(RuleBasedEngine ruleBasedEngine, ValidationService validationService, RateLimitService rateLimitService, MetricsService metricsService) {
        this.ruleBasedEngine = ruleBasedEngine;
        this.validationService = validationService;
        this.rateLimitService = rateLimitService;
        this.metricsService = metricsService;
    }

    @ModelAttribute("conversationContext")
    public ConversationContext initializeConversation() {
        return new ConversationContext();
    }


    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        String sessionId = session.getId();
        
        // Rate limiting
        if (!rateLimitService.isAllowed(sessionId)) {
            logger.warn("Rate limit exceeded for session: {}", sessionId);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Too many requests. Please wait a moment.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        String userMessage = request.get("message");
        
        // Validation
        if (!validationService.isValidMessage(userMessage)) {
            logger.warn("Invalid message from session: {}", sessionId);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid or too long message.");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Sanitize input
        userMessage = validationService.sanitizeInput(userMessage);
        
        logger.info("Processing message from session {}: {}", sessionId, userMessage);

        // Get or create conversation context
        ConversationContext context = (ConversationContext)
                session.getAttribute("conversationContext");
        if (context == null) {
            context = new ConversationContext();
            session.setAttribute("conversationContext", context);
            metricsService.recordNewSession(sessionId);
        }
        
        // Record metrics
        metricsService.recordMessage(sessionId);

        // Add user message to conversation
        context.addMessage("USER", userMessage);

        // Process the message and get response
        String botResponse = processChatMessage(userMessage, context);
        
        logger.info("Bot response for session {}: {}", sessionId, botResponse);

        // Add bot response to conversation
        context.addMessage("BOT", botResponse);
        
        logger.debug("Session {} stats - Messages: {}, Total sessions: {}", 
                    sessionId, metricsService.getSessionMessageCount(sessionId), metricsService.getTotalSessions());

        return ResponseEntity.ok(context);
    }

    private String processChatMessage(String message, ConversationContext context) {
        String response =  "";

        if (message.equalsIgnoreCase("start")) {
            return "Hello! How can I help you today?";
        } else if (message.equalsIgnoreCase("clear")) {
            context.clear();
            return "Conversation has been reset. \nHow can I help you today?";
        } else {
            /**  Set the value of the variable "response"
             *          to the value returned by the processUserInput method of the RuleBasedEngine class.
             *          The method should be called with the message and context as arguments.
             **/
            response = ruleBasedEngine.processUserInput(message, context);

            return  response;
        }

    }



}