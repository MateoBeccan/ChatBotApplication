package com.ajsd.chatbot.controller;

import com.ajsd.chatbot.model.ConversationContext;
import com.ajsd.chatbot.service.RuleBasedEngine;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@SessionAttributes("conversationContext")
public class ChatbotController {

    private final RuleBasedEngine ruleBasedEngine;

    public ChatbotController(RuleBasedEngine ruleBasedEngine) {
        this.ruleBasedEngine = ruleBasedEngine;
    }

    @ModelAttribute("conversationContext")
    public ConversationContext initializeConversation() {
        return new ConversationContext();
    }


    @PostMapping("/chat")
    public ResponseEntity<ConversationContext> chat(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        String userMessage = request.get("message");

        // Get or create conversation context
        ConversationContext context = (ConversationContext)
                session.getAttribute("conversationContext");
        if (context == null) {
            context = new ConversationContext();
            session.setAttribute("conversationContext", context);
        }

        // Add user message to conversation
        context.addMessage("USER", userMessage);

        // Process the message and get response
        String botResponse = processChatMessage(userMessage, context);

        // Add bot response to conversation
        context.addMessage("BOT", botResponse);

        return ResponseEntity.ok(context);
    }

    private String processChatMessage(String message, ConversationContext context) {
        String response =  "";

        if (message.equalsIgnoreCase("start")) {
            return "Hello! How can I help you today?";
        } else if (message.equalsIgnoreCase("clear")) {
            context.clear();
            return "Conversation has been reset.";
        }  else {
            /**  Set the value of the variable "response"
             *          to the value returned by the processUserInput method of the RuleBasedEngine class.
             *          The method should be called with the message and context as arguments.
             **/
            response = ruleBasedEngine.processUserInput(message, context);


            return  response;
        }

    }



}