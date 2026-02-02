# Implement DeepSeek Chatbot Integration

## 1. Backend Implementation (Spring Boot)

### Create Chat Service & Controller

* **Module**: `community-club`

* **Service**: `com.cloud.community.club.service.ChatService`

  * Implement `chat(String message)` method.

  * Use `RestTemplate` to call DeepSeek API (`https://api.deepseek.com/chat/completions`).

  * Construct the JSON payload required by DeepSeek (OpenAI compatible format).

* **Controller**: `com.cloud.community.club.controller.ChatController`

  * Endpoint: `POST /api/club/chat`

  * Request Body: `{"message": "user question"}`

  * Response: Standard `Result<String>` containing the AI's reply.

### Configuration

* **File**: `community-gateway/src/main/resources/application.properties`

* **Settings**:

  * `deepseek.api.url`: `https://api.deepseek.com/chat/completions`

  * `deepseek.api.key`: Add a placeholder (You will need to replace this with your actual key).

## 2. Frontend Implementation (Vue.js)

### Create Chat Component

* **File**: `frontend/src/components/ChatWidget.vue`

* **Features**:

  * Floating button (bottom-right corner).

  * Expandable chat window.

  * Message history list (User vs. AI).

  * Input field with "Send" button.

  * Integration with backend `/api/club/chat` endpoint.

### Integrate into Club Detail Page

* **File**: `frontend/src/views/student/ClubDetailView.vue`

* **Action**: Import and register `ChatWidget` component to appear on the club detail page.

## 3. Verification

* Verify the chat UI appears on the Club Detail page.

* Test sending a message and ensure the backend attempts to call the DeepSeek API.

* (Note: Without a valid API key, the actual call will fail, but we can verify the flow).

