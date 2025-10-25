package com.ajsd.chatbot.service;

import com.ajsd.chatbot.config.CountryDataLoader;
import com.ajsd.chatbot.model.ConversationContext;
import com.ajsd.chatbot.service.MedicalService;
import com.ajsd.chatbot.service.IntelligenceService;
import com.ajsd.chatbot.service.ContextAnalyzer;
import org.springframework.stereotype.Component;

/*
    This class implements the Rule based engine to
    answer questions about countries and communicate
    with the user. The rules are simple.

    1. Chatbot says what do you want to learn.

    2. If user says something like "I want to learn about countries", or "I do not know"
       or "teach me how to use this" then the chatbot will say
       "I can teach about countries, their capitals, national animals and national flowers.
       What do you want to learn about?". If the chatbot does not understand the question,
       it will say "I do not understand. Please ask me something else. I can only teach about
       countries, their capitals, national animals and national flowers. Shall we do that?"

    3. After the user confirms that they want to learn about countries, the chatbot will say
       "Great! I can teach you about countries, their capitals, national animals and national flowers.
       What country do you want to learn about?".

     4.  If the user does not specify a country, the chatbot will say "I do not understand. Please
          list a give me a country to talk about. What do you want to :
           A. give the name of a country in full
           B. give the beginning of the name of a country
           C. give the ending of the name of a country
           D: give me part of the name of a country
           E: Have me list all the countries which are available

    5. If the user is wrong in listing a giving the correct option
       for Step 4, then the chatbot will say "I do not understand. Please list a give me a country to talk about.
       What do you want to :
           A. give the name of a country in full
           B. give the beginning of the name of a country
           C. give the ending of the name of a country
           D: give me part of the name of a country
           E: Have me list all the countries which are available"

    6. When given the name of a country, the chatbot will say "Great! I know about <country name>.
       What do you want to learn about <country name>?
           A. learn about the capital
           B. learn about the national animal
           C. learn about the national flower
           D. learn about all of the above
           E. Choose another country
       ".

    7. If the user is wrong in listing a giving the correct option
       for options given in 6, the chatbot will say "I do not understand. Please choose one of the following:
           A. learn about the capital
           B. learn about the national animal
           C. learn about the national flower
           D. learn about all of the above
           E. Choose another country
       "

    8. If the user wants to learn about one of the valid options under 6 or 7 - A to D, then
       the chatbot replies with the correct option value,  for example
       "The capital of <country name> is <capital name>".
       and then says. "What else do you want to learn about <country name>?" and lists the options
       from A to E of option 6.  If the user wants to learn about another country, the chatbot will
       say "What country do you want to learn about?" and then go back to step 4.

 */



@Component
public class RuleBasedEngine {

    private final ChatbotService chatbotService;
    private final MedicalService medicalService;
    private final IntelligenceService intelligenceService;
    private final ContextAnalyzer contextAnalyzer;
    
    private static final String COUNTRY_OPTIONS_STRING = "What do you want to learn about it?\n" +
            "A. learn about the capital\n" +
            "B. learn about the national animal\n" +
            "C. learn about the national flower\n" +
            "D. learn about all of the above\n" +
            "E. Choose another country";
            
    private static final String MEDICAL_OPTIONS_STRING = "What do you want to learn about it?\n" +
            "A. learn about symptoms\n" +
            "B. learn about causes\n" +
            "C. learn about treatment\n" +
            "D. learn about prevention\n" +
            "E. learn about all of the above\n" +
            "F. Choose another condition";

    public RuleBasedEngine(ChatbotService chatbotService, MedicalService medicalService, IntelligenceService intelligenceService, ContextAnalyzer contextAnalyzer) {
        this.chatbotService = chatbotService;
        this.medicalService = medicalService;
        this.intelligenceService = intelligenceService;
        this.contextAnalyzer = contextAnalyzer;
    }

    public String processUserInput(String userInput, ConversationContext context) {
        // Handle help command at any step
        if (userInput.toLowerCase().contains("help")) {
            return getHelpMessage();
        }
        
        // Check for intelligent responses first
        String smartResponse = intelligenceService.generateSmartResponse(userInput, context);
        if (smartResponse != null) {
            return smartResponse;
        }
        
        // Analyze context for deeper understanding
        String contextualResponse = contextAnalyzer.analyzeUserIntent(userInput, context);
        if (contextualResponse != null) {
            return contextualResponse;
        }
        
        switch (context.getCurrentStep()) {
            case "ASK_INTENT":
                return handleAskIntent(userInput, context);
            case "SELECT_TOPIC":
                return handleSelectTopic(userInput, context);
            case "SELECT_COUNTRY":
                return handleSelectCountry(userInput, context);
            case "SELECT_CONDITION":
                return handleSelectCondition(userInput, context);
            case "CHOOSE_OPTION":
                return handleChooseOption(userInput, context);
            default:
                context.setCurrentStep("ASK_INTENT");
                return "I do not understand. \nLet's start again. \nWhat would you like to learn about?";
        }
    }

    private String handleAskIntent(String userInput, ConversationContext context) {
        if (userInput.toLowerCase().contains("teach")) {
            context.setCurrentStep("SELECT_TOPIC");
            return "I can teach you about different topics. What would you like to learn about?\n" +
                   "A. Countries (capitals, animals, flowers)\n" +
                   "B. Medicine (conditions, symptoms, treatments)";
        } else if (userInput.toLowerCase().contains("help")) {
            return getHelpMessage();
        }
        return "I do not understand. \nPlease ask me something else. \nType 'teach' to see what I can help you with.";
    }

    private String handleSelectCountry(String countryNameInput, ConversationContext context) {
        boolean countryExists = chatbotService.isValidCountry(countryNameInput);
        if (countryExists) {
            context.setSelectedCountry(countryNameInput);
            context.setCurrentStep("CHOOSE_OPTION");
            return "Great! I know about that country.\n" + COUNTRY_OPTIONS_STRING;
        }
        return "I do not understand. \nPlease provide a valid country.";
    }

    private String handleChooseOption(String userInput, ConversationContext context) {
        if ("COUNTRIES".equals(context.getCurrentTopic())) {
            return handleCountryOptions(userInput, context);
        } else if ("MEDICINE".equals(context.getCurrentTopic())) {
            return handleMedicalOptions(userInput, context);
        }
        return "Something went wrong. Please start over.";
    }
    
    private String handleCountryOptions(String userInput, ConversationContext context) {
        String country = context.getSelectedCountry();
        String baseResponse;
        
        if (userInput.equalsIgnoreCase("A")) {
            baseResponse = "The capital of " + country + " is " + chatbotService.getCapital(country) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "COUNTRIES", country, context) + "\n\n" + COUNTRY_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("B")) {
            baseResponse = "The national animal of " + country + " is " + chatbotService.getNationalAnimal(country) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "COUNTRIES", country, context) + "\n\n" + COUNTRY_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("C")) {
            baseResponse = "The national flower of " + country + " is " + chatbotService.getNationalFlower(country) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "COUNTRIES", country, context) + "\n\n" + COUNTRY_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("D")) {
            baseResponse = "The capital of " + country + " is " + chatbotService.getCapital(country) + ".\n" +
                    "The national animal of " + country + " is " + chatbotService.getNationalAnimal(country) + ".\n" +
                    "The national flower of " + country + " is " + chatbotService.getNationalFlower(country) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "COUNTRIES", country, context) + "\n\n" + COUNTRY_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("E")) {
            context.setCurrentStep("SELECT_COUNTRY");
            return "What country do you want to learn about?";
        } else {
            return "I do not understand. \nPlease choose one of the following:\n" + COUNTRY_OPTIONS_STRING;
        }
    }
    
    private String handleMedicalOptions(String userInput, ConversationContext context) {
        String condition = context.getSelectedCondition();
        String baseResponse;
        
        if (userInput.equalsIgnoreCase("A")) {
            baseResponse = "Symptoms of " + condition + ": " + medicalService.getSymptoms(condition) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "MEDICINE", condition, context) + "\n\n" + MEDICAL_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("B")) {
            baseResponse = "Causes of " + condition + ": " + medicalService.getCauses(condition) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "MEDICINE", condition, context) + "\n\n" + MEDICAL_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("C")) {
            baseResponse = "Treatment for " + condition + ": " + medicalService.getTreatment(condition) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "MEDICINE", condition, context) + "\n\n" + MEDICAL_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("D")) {
            baseResponse = "Prevention of " + condition + ": " + medicalService.getPrevention(condition) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "MEDICINE", condition, context) + "\n\n" + MEDICAL_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("E")) {
            baseResponse = "Complete information about " + condition + ":\n" +
                    "Symptoms: " + medicalService.getSymptoms(condition) + ".\n" +
                    "Causes: " + medicalService.getCauses(condition) + ".\n" +
                    "Treatment: " + medicalService.getTreatment(condition) + ".\n" +
                    "Prevention: " + medicalService.getPrevention(condition) + ".";
            return intelligenceService.enhanceResponse(baseResponse, "MEDICINE", condition, context) + "\n\n" + MEDICAL_OPTIONS_STRING;
        } else if (userInput.equalsIgnoreCase("F")) {
            context.setCurrentStep("SELECT_CONDITION");
            return "Available conditions: diabetes, hypertension, asthma, migraine, depression.\nWhat condition do you want to learn about?";
        } else {
            return "I do not understand. \nPlease choose one of the following:\n" + MEDICAL_OPTIONS_STRING;
        }
    }
    
    private String handleSelectTopic(String userInput, ConversationContext context) {
        if (userInput.equalsIgnoreCase("A") || userInput.toLowerCase().contains("countries") || userInput.toLowerCase().contains("country")) {
            context.setCurrentTopic("COUNTRIES");
            context.setCurrentStep("SELECT_COUNTRY");
            return "Great! I can teach you about countries, their capitals, national animals, and national flowers.\n What country do you want to learn about?";
        } else if (userInput.equalsIgnoreCase("B") || userInput.toLowerCase().contains("medicine") || userInput.toLowerCase().contains("medical")) {
            context.setCurrentTopic("MEDICINE");
            context.setCurrentStep("SELECT_CONDITION");
            return "Great! I can teach you about medical conditions.\n Available conditions: diabetes, hypertension, asthma, migraine, depression.\n What condition do you want to learn about?";
        }
        return "I do not understand. Please choose:\n" +
               "A. Countries (capitals, animals, flowers)\n" +
               "B. Medicine (conditions, symptoms, treatments)";
    }
    
    private String handleSelectCondition(String conditionInput, ConversationContext context) {
        boolean conditionExists = medicalService.isValidCondition(conditionInput);
        if (conditionExists) {
            context.setSelectedCondition(conditionInput.toLowerCase());
            context.setCurrentStep("CHOOSE_OPTION");
            return "Great! I know about " + conditionInput + ".\n" + MEDICAL_OPTIONS_STRING;
        }
        return "I do not understand that condition. \nAvailable conditions: diabetes, hypertension, asthma, migraine, depression.\nPlease provide a valid condition.";
    }
    
    private String getHelpMessage() {
        return "🤖 **AJSD Chatbot Help**\n\n" +
               "**How to use the chatbot:**\n" +
               "1. Type 'teach' to begin\n" +
               "2. Type 'clear' to reset the conversation\n" +
               "3. Type 'help' to see this message\n\n" +
               "**What I can teach you:**\n" +
               "• Countries: capitals, national animals, national flowers\n" +
               "• Medicine: symptoms, causes, treatments, prevention\n\n" +
               "**Available Topics:**\n" +
               "• Countries: Any country name\n" +
               "• Medical: diabetes, hypertension, asthma, migraine, depression\n\n" +
               "**Tips:**\n" +
               "• Choose A or B for topic selection\n" +
               "• Use A-F options during conversations\n" +
               "• Type exact condition names for medical topics\n\n" +
               "Ready to start? Type 'teach' to begin.";
    }


}