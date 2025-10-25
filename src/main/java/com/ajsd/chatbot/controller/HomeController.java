package com.ajsd.chatbot.controller;

import com.ajsd.chatbot.model.ConversationContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    @RequestMapping("/")
    public String home(Model model, HttpSession session) {
        ConversationContext context = (ConversationContext) session.getAttribute("conversationContext");
        if (context == null) {
            context = new ConversationContext();
            session.setAttribute("conversationContext", context);
        }
        model.addAttribute("conversationContext", context);
        return "index";
    }
}
