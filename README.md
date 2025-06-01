# WhatsApp Flows Spring Boot Application
###Note that I used chatGPT to generate this readme. Please update parts that may be incorrect
A Spring Boot (WebFlux) example demonstrating both **encrypted (endpoint-powered)** and **unencrypted (builder-only)** WhatsApp Flows using the WhatsApp Cloud API. This README outlines:

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Configuration](#configuration)

    * [`application.yml`](#applicationyml)
    * [Encryption Keys](#encryption-keys)
    * [Security Configuration](#security-configuration)
4. [Building & Running](#building--running)
5. [WhatsApp Cloud API Setup](#whatsapp-cloud-api-setup)

    * [Webhook Registration](#webhook-registration)
    * [Flow Creation & Encryption Setup](#flow-creation--encryption-setup)
6. [Controller Endpoints](#controller-endpoints)

    * [`WebhookController`](#webhookcontroller)
    * [`FlowEndpointController`](#flowendpointcontroller)
7. [Dispatching Incoming Webhooks](#dispatching-incoming-webhooks)
8. [Sending Outbound Messages](#sending-outbound-messages)
9. [Flow Service Logic](#flow-service-logic)
10. [DTOs & Models](#dtos--models)
11. [Security Configuration Details](#security-configuration-details)
12. [Testing Locally with ngrok](#testing-locally-with-ngrok)
13. [Example Flow Lifecycle](#example-flow-lifecycle)
14. [References](#references)

---

## Prerequisites

* **Java 17** (JDK 17)
* **Maven 3.6+**
* A **WhatsApp Business Account**/Business Manager (to configure Flows and webhooks)
* A **public HTTPS URL** (e.g., via [ngrok](https://ngrok.com/)) for webhook callbacks and, if using encrypted Flows, for data-exchange endpoints
* **Bouncy Castle** dependency (included via Maven)
* An **RSA key pair** (private key in PKCS#8 unencrypted PEM; public key in PEM) stored under `src/main/resources/keys/`
* A **Cloud API access token** and your **phone\_number\_id** from the WhatsApp Cloud API

---

## Project Structure

Below is a high-level overview of the package layout. See each source file for detailed implementation.

```
src
└── main
    ├── java
    │   └── com.example.whatsappflows
    │       ├── WhatsAppFlowsApplication.java         # Main Spring Boot app
    │       │
    │       ├── config
    │       │   ├── EncryptionConfig.java             # RSA key paths (@ConfigurationProperties)
    │       │   ├── WhatsAppApiConfig.java            # WhatsApp Cloud API settings (@ConfigurationProperties)
    │       │   └── WebFluxSecurityConfig.java        # Security configuration for WebFlux
    │       │
    │       ├── controller
    │       │   ├── WebhookController.java            # Handles /webhook Cloud API callbacks
    │       │   └── FlowEndpointController.java       # Handles /webhook/flow/* for encrypted Flows
    │       │
    │       ├── dispatcher
    │       │   └── WebhookDispatcher.java            # Routes incoming webhooks to handlers
    │       │
    │       ├── handler
    │       │   ├── MessageHandler.java               # Processes incoming messages & Flow replies
    │       │   └── StatusHandler.java                # Processes incoming status updates
    │       │
    │       ├── client
    │       │   └── WhatsAppApiClient.java            # Wraps HTTP calls to WhatsApp Cloud API
    │       │
    │       ├── service
    │       │   ├── FlowService.java                  # Core Flow orchestration (encrypted & unencrypted)
    │       │   ├── EncryptionService.java            # Loads RSA keys & wraps crypto utilities
    │       │   ├── MessageService.java               # Sends text, templates, and raw Flow payloads
    │       │   └── TemplateService.java              # Decides which template to send based on input
    │       │
    │       ├── crypto
    │       │   └── FlowCryptoUtils.java              # RSA + AES/GCM encryption/decryption utilities
    │       │
    │       ├── dto
    │       │   ├── FlowEncryptedPayload.java         # { encrypted_flow_data, encrypted_aes_key, initial_vector }
    │       │   ├── FlowDataExchangePayload.java      # { version, action, screen, data, flow_token }
    │       │   ├── FlowResponsePayload.java          # Sealed interface (Next/Final responses)
    │       │   ├── NextScreenResponsePayload.java    # { screen, data } for intermediate steps
    │       │   ├── FinalScreenResponsePayload.java   # { screen="SUCCESS", data.extension_message_response }
    │       │   ├── FlowStatusRequest.java            # { flow_token, status, timestamp, data }
    │       │   ├── ErrorNotificationRequest.java     # { version, flow_token, action, data.error, data.error_message }
    │       │   ├── ErrorNotificationResponse.java    # { data: { acknowledged: true } }
    │       │   ├── HealthCheckRequest.java           # { version, action="ping" }
    │       │   └── HealthCheckResponse.java          # { data: { status: "active" } }
    │       │
    │       └── model
    │           └── BookingState.java                 # POJO: { step, destination, date }
    │
    └── resources
        ├── application.yml                            # Main configuration
        └── keys
            ├── private_key.pem                        # RSA private key (PKCS#8)
            └── public_key.pem                         # RSA public key (PEM)
```

> **Tip:** Almost all Java classes have accompanying Javadoc in the source—refer to those for complete details.

---

## Configuration

### `application.yml`

```yaml
spring:
  application:
    name: whatsapp-flows-app

# ----------------- WhatsApp Cloud API Settings -----------------
whatsapp:
  api:
    phoneNumberId: "123456789012345"        # Your WhatsApp Business Phone Number ID
    accessToken: "EAAJZC..."                # Your Cloud API access token
    baseUrl: "https://graph.facebook.com"

# ----------------- Encryption (for endpoint‐powered flows) -----------------
encryption:
  privateKeyPath: "keys/private_key.pem"   # Classpath to RSA private key (PKCS#8 unencrypted PEM)
  publicKeyPath: "keys/public_key.pem"     # Classpath to RSA public key (PEM)

# ----------------- WebFlux Security (optional) -----------------
# To disable reactive security auto‐configuration, uncomment:
# spring:
#   autoconfigure:
#     exclude:
#       - org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration
#       - org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration

# Or keep Security and allow only /webhook/** (see WebFluxSecurityConfig)
```

* **`whatsapp.api.*`**: Set your `phoneNumberId` and `accessToken`.
* **`encryption.*.privateKeyPath/publicKeyPath`**: Point to the RSA key files under `src/main/resources/keys/`.
* **Security settings**: Either completely disable WebFlux Security or keep it and permit only `/webhook/**`.

---

### Encryption Keys

1. **Generate RSA key pair** (2048-bit minimum) using OpenSSL:

   ```bash
   # Private key (PKCS#8 unencrypted PEM)
   openssl genrsa -out src/main/resources/keys/private_key.pem 2048

   # Public key (PEM)
   openssl rsa -in src/main/resources/keys/private_key.pem -pubout \
     -out src/main/resources/keys/public_key.pem
   ```
2. **Ensure** `encryption.privateKeyPath` and `encryption.publicKeyPath` in `application.yml` match those locations.
3. **Upload** `public_key.pem` in Meta’s Flow Manager when configuring your endpoint-powered Flow.

---

### Security Configuration

By including `spring-boot-starter-security`, Spring Boot auto-configures WebFlux Security and locks down all endpoints by default (redirecting to a login form). You can override that in two main ways:

#### A) Disable Security Entirely

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration
      - org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration
```

No additional Java config is needed—every endpoint (including `/webhook`) is publicly accessible.

#### B) Permit Only `/webhook/**`

Create a class `WebFluxSecurityConfig.java` under `com.example.whatsappflows.config`:

```java
package com.example.whatsappflows.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {
    @Bean
    public SecurityWebFilterChain securitygWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/webhook/**").permitAll()
                .anyExchange().authenticated()
            );
        return http.build();
    }
}
```

* **Allows** anonymous access to `/webhook/**`.
* **Secures** any other endpoint (if you have an admin UI, etc.).
* **Disables** CSRF for JSON-only endpoints.

---

## Building & Running

1. **Clone or copy** this project locally.
2. **Place RSA keys** (private/public) under `src/main/resources/keys/` (or update `application.yml` to your paths).
3. **Update `application.yml`**: set `whatsapp.api.phoneNumberId`, `whatsapp.api.accessToken`, and encryption key paths. Optionally adjust security settings.
4. **Build with Maven**:

   ```bash
   mvn clean package -DskipTests
   ```
5. **Run the application**:

   ```bash
   mvn spring-boot:run
   ```

   or

   ```bash
   java -jar target/whatsapp-flows-app-0.0.1-SNAPSHOT.jar
   ```
6. By default, the server listens on port **8080**. To use a different port (e.g., 3000):

   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=3000"
   ```

---

## WhatsApp Cloud API Setup

### Webhook Registration

1. Sign in to Meta Developer Portal and select your WhatsApp Cloud API app.
2. Go to **Webhooks → Add Callback URL**, set:

   ```
   Callback URL:  https://<YOUR_NGROK_ID>.ngrok.io/webhook
   Verify Token:  zap_verify
   ```
3. **Subscribe** to the following fields:

    * `messages`
    * `statuses`

Meta will send a GET request to `/webhook?hub.mode=subscribe&hub.verify_token=zap_verify&hub.challenge=<challenge>`. The `WebhookController.verifyWebhook(...)` responds with the `hub.challenge`.

### Flow Creation & Encryption Setup

1. In **Meta Business Manager → WhatsApp → Flows** (Flow Manager), create a new Flow.
2. For an **encrypted, endpoint-powered** Flow:

    * Choose “Use an endpoint (Data Exchange).”
    * Set Data Exchange URL to:

      ```
      https://<YOUR_NGROK_ID>.ngrok.io/webhook/flow/data-exchange
      ```
    * Toggle **“Enable State Encryption.”**
    * Upload your `public_key.pem`.
    * Define initial screens (optional; your backend can also generate them).
    * **Publish** and note the Flow ID.
3. For an **unencrypted, builder-only** Flow:

    * Use the visual Flow Builder to create screens entirely in Meta.
    * Do **not** specify an endpoint URL. In that case, WhatsApp’s client renders the Flow locally, and each user action sends an `nfm_reply` to your `/webhook` callback.

---

## Controller Endpoints

### `WebhookController`

Located at `com.example.whatsappflows.controller.WebhookController`.

* **`GET /webhook`**: Verifies the webhook handshake (responds with `hub.challenge`).
* **`POST /webhook`**: Receives all Cloud API webhooks (messages, statuses, Flow `nfm_reply`). Delegates to `WebhookDispatcher`.

### `FlowEndpointController`

Located at `com.example.whatsappflows.controller.FlowEndpointController`.

* **`POST /webhook/flow/data-exchange`**
  Handled by `FlowService.handleExchange(...)`: decrypts incoming Flow payloads, processes the next screen or final response, re-encrypts state, and returns JSON.

* **`POST /webhook/flow/status-update`**
  Receives Flow status updates (`completed`, `expired`, `failed`). Currently logs or responds with 200 OK.

* **`POST /webhook/flow/error-notification`**
  Receives error notifications when WhatsApp reports an invalid response payload. Returns `{ data: { acknowledged: true } }`.

* **`POST /webhook/flow/ping`**
  Responds to health checks. Returns `{ data: { status: "active" } }`.

---

## Dispatching Incoming Webhooks

### `WebhookDispatcher`

Located at `com.example.whatsappflows.dispatcher.WebhookDispatcher`.

* Inspects the JSON under `entry[0].changes[0].value`.
* If it contains `messages`, calls `MessageHandler.handle(...)`.
* If it contains `statuses`, calls `StatusHandler.handle(...)`.

### `MessageHandler`

Located at `com.example.whatsappflows.handler.MessageHandler`.

* Loops through each incoming message in the JSON array.
* If `type="text"`, extracts the text and `from` number, then calls `MessageService.processIncomingText(from, text)`.
* If `type="interactive"`, and `interactive.type="nfm_reply"`, extracts `nfm_reply.response_json` (plain JSON).

    * If that JSON has `"version"` and `"action"`, it’s an intermediate builder-only Flow step. Deserializes it into `FlowDataExchangePayload` and calls `FlowService.handlePlainExchange(...)`, then sends the returned payload via `MessageService.sendRawFlowMessage(...)`.
    * Otherwise, treats it as the final Flow completion, converts to `Map<String,Object>`, and calls `FlowService.handlePlainCompletion(...)`.

### `StatusHandler`

Located at `com.example.whatsappflows.handler.StatusHandler`.

* Loops through each incoming status in `statuses[]`.
* Logs or processes message‐delivery/read updates as needed.

---

## Sending Outbound Messages

### `WhatsAppApiClient`

Located at `com.example.whatsappflows.client.WhatsAppApiClient`.

* Wraps a preconfigured `WebClient` bean pointing to:

  ```
  https://graph.facebook.com/v15.0/{phoneNumberId}/messages
  ```

  with default headers:

  ```
  Authorization: Bearer <accessToken>
  Content-Type: application/json
  ```

* **`sendText(to, text)`**: Sends a simple plain‐text message.

* **`sendTemplate(templateName, languageCode, to, components)`**: Sends a template message with components.

* **`sendRawMessage(payload)`**: Sends an arbitrary JSON payload (used for raw Flow interactive messages).

### `MessageService`

Located at `com.example.whatsappflows.service.MessageService`.

* **`processIncomingText(from, messageText)`**:

    1. Calls `TemplateService.chooseTemplate(messageText)`.
    2. If a template is returned, sends it via `apiClient.sendTemplate(...)`.
    3. Else, if `messageText.equalsIgnoreCase("Book Ticket")`, initiates an unencrypted Flow by building the raw interactive JSON and calling `apiClient.sendRawMessage(...)`.
    4. Otherwise, sends a fallback text reply.

* **`sendRawFlowMessage(from, responseMap)`**: Adds `"to": from` and calls `apiClient.sendRawMessage(responseMap)` to send intermediate Flow payloads.

* **`sendTextMessage(from, text)`**: Calls `apiClient.sendText(from, text)` to send a simple text.

### `TemplateService`

Located at `com.example.whatsappflows.service.TemplateService`.

* **`chooseTemplate(messageText)`** returns an `Optional<TemplatePayload>`. Example triggers:

    * If the text contains `"reminder"`, returns a **reminder** template with body parameters.
    * If the text contains `"apt"` or `"appointment"`, returns a **name\_dob\_capture** template with header image and a button (sub\_type=`flow`).
    * Otherwise, returns a **hello\_world** template.

`TemplatePayload` holds:

```java
record TemplatePayload(
    String name,
    String languageCode,
    List<Map<String,Object>> components
) {}
```

---

## Flow Service Logic

All Flow orchestration (encrypted and unencrypted) lives in `com.example.whatsappflows.service.FlowService`. Below is an outline of its key methods.

### Encrypted, Endpoint-Powered Flow (`handleExchange`)

```java
public Mono<Map<String,Object>> handleExchange(FlowEncryptedPayload encryptedPayload) {
    return Mono.fromCallable(() -> {
        // 1. Decrypt with RSA/AES using EncryptionService
        DecryptionResult dr = encryptionService.decryptPayload(
            encryptedPayload.getEncryptedFlowData(),
            encryptedPayload.getEncryptedAesKey(),
            encryptedPayload.getInitialVector()
        );

        // 2. Parse decrypted JSON into FlowDataExchangePayload
        FlowDataExchangePayload request = objectMapper.readValue(dr.getClearJson(), FlowDataExchangePayload.class);

        // 3. Rebuild or initialize BookingState from request.getScreen() & request.getData()
        BookingState state = rebuildState(request);

        // 4. Decide next screen or final:
        //    INIT    → showInitialScreen(state)
        //    BACK    → showBackScreen(state)
        //    data_exchange → processSubmission(request, state)
        FlowResponsePayload ui;
        switch (request.getAction()) {
            case "INIT":          ui = showInitialScreen(state);     break;
            case "BACK":          ui = showBackScreen(state);        break;
            case "data_exchange": ui = processSubmission(request, state); break;
            default: throw new IllegalArgumentException("Unknown action: " + request.getAction());
        }

        // 5. If ui is FinalScreenResponsePayload, insert flow_token + validate()
        if (ui instanceof FinalScreenResponsePayload finalUi) {
            finalUi.getData().getParams().put("flow_token", request.getFlow_token());
            finalUi.getData().validate();
        }

        // 6. Serialize & re-encrypt updated BookingState JSON
        String newStateJson = objectMapper.writeValueAsString(state);
        String encryptedState = encryptionService.encryptState(
            newStateJson, dr.getAesKey(), dr.getIv()
        );

        // 7. Build response Map:
        //    {
        //      "version":"3.0",
        //      "flow_token":"<TOKEN>",
        //      "data":"<BASE64_ENCRYPTED_STATE>",
        //      "screen": { … NextScreenResponsePayload or FinalScreenResponsePayload … },
        //      "close": true (if final)
        //    }
        Map<String,Object> response = new java.util.LinkedHashMap<>();
        response.put("version", "3.0");
        response.put("flow_token", request.getFlow_token());
        response.put("data", encryptedState);
        response.put("screen", ui);
        if (ui instanceof FinalScreenResponsePayload) {
            response.put("close", true);
        }
        return response;
    });
}
```

* **`FlowEncryptedPayload`** has fields:

  ```json
  {
    "encrypted_flow_data": "<Base64_AES_GCM_CIPHERTEXT>",
    "encrypted_aes_key": "<Base64_RSA_OAEP_KEY>",
    "initial_vector": "<Base64_IV_BYTES>"
  }
  ```
* **`FlowDataExchangePayload`** (decrypted) looks like:

  ```json
  {
    "version": "3.0",
    "action": "INIT" | "BACK" | "data_exchange",
    "screen": "<CURRENT_SCREEN>",
    "data": { /* user‐entered fields */ },
    "flow_token": "<FLOW_TOKEN>"
  }
  ```
* **`BookingState`** (`model/BookingState.java`) holds fields:

  ```java
  private String step;
  private String destination;
  private String date;
  ```
* **`NextScreenResponsePayload`** (for intermediate steps) has:

  ```json
  {
    "screen": "<NEXT_SCREEN_NAME>",
    "data": { /* key/value pairs for that screen */ }
  }
  ```
* **`FinalScreenResponsePayload`** has:

  ```json
  {
    "screen": "SUCCESS",
    "data": {
      "extension_message_response": {
        "params": {
          "flow_token": "<FLOW_TOKEN>",
          /* ... any other final params ... */
        }
      }
    }
  }
  ```
* After choosing `ui`, the updated `BookingState` is serialized to JSON and AES/GCM–encrypted (using `dr.getAesKey()` and a flipped IV), then returned.

---

### Unencrypted / Builder-Only Flow

#### Intermediate Steps (`handlePlainExchange`)

```java
public Mono<Map<String,Object>> handlePlainExchange(FlowDataExchangePayload request) {
    return Mono.fromCallable(() -> {
        String flowToken = request.getFlow_token();
        BookingState state = plainStateStore.computeIfAbsent(flowToken, t -> new BookingState());

        // On INIT: set state.step = "choose_destination"
        // On data_exchange: 
        //   if state.step == "choose_destination": read request.getData().get("screen_0_Name_0"), set state.destination, state.step="choose_date"
        //   if state.step == "choose_date": read request.getData().get("screen_0_DoB_1"), set state.date, state.step="confirm"
        //   if state.step == "confirm": set state.step="completed"
        // (full logic in source FlowService.java)

        // Build the next interactive payload for each step:
        // - choose_destination → prompt “Select your destination:”
        // - choose_date      → prompt “Select travel date:”
        // - confirm          → prompt “Confirm booking for X on Y?”
        Map<String,Object> interactive = …; // see source for exact Map structure

        // Build and return:
        // {
        //   "messaging_product":"whatsapp",
        //   "type":"interactive",
        //   "interactive": { … above Map … }
        // }
        return Map.of(
            "messaging_product", "whatsapp",
            "type", "interactive",
            "interactive", interactive
        );
    });
}
```

* **`FlowDataExchangePayload`** for unencrypted builder-only Flows has the same schema as above (JSON returned in `nfm_reply.response_json`).
* You update the **same** `BookingState` logic as in the encrypted flow.
* Return a `Map<String,Object>` representing exactly the Cloud API `/messages` JSON.
* The caller (`MessageHandler`) then invokes `messageService.sendRawFlowMessage(from, thatMap)` to POST it.

#### Final/Completion (`handlePlainCompletion`)

```java
public void handlePlainCompletion(Map<String,Object> finalParams, String from) {
    String flowToken = finalParams.getOrDefault("flow_token", "").toString();
    // Log or persist finalParams (e.g. appointment_date, confirmation_id)
    // Send a confirmation text (or template) back to the user via:
    apiClient.sendText(from, "✅ Booking confirmed! Details: …").subscribe();
    // Remove state from in-memory store
    plainStateStore.remove(flowToken);
}
```

* **Called** when `nfm_reply.response_json` has no `"version"`/`"action"` fields.
* Final JSON can be anything you defined in the Flow Builder’s “Complete” action or from your encrypted-Flow endpoint.
* After handling backend logic, send a confirmation to the user via `apiClient.sendText(...)` or `messageService.sendTemplate(...)`.

---

## DTOs & Models

All DTOs and models live under `src/main/java/com/example/whatsappflows/dto` and `model`. Below is a summary—see each file’s Javadoc for complete details.

* **`FlowEncryptedPayload.java`**

  ```java
  class FlowEncryptedPayload {
      String encryptedFlowData;   // "encrypted_flow_data"
      String encryptedAesKey;     // "encrypted_aes_key"
      String initialVector;       // "initial_vector"
  }
  ```

* **`FlowDataExchangePayload.java`**

  ```java
  class FlowDataExchangePayload {
      String version;             // "3.0"
      String action;              // "INIT" | "BACK" | "data_exchange"
      String screen;              // Screen name
      Map<String,Object> data;    // Key/value pairs from the user
      String flow_token;          // Flow token (UUID)
  }
  ```

* **`FlowResponsePayload.java`** (sealed)

    * Implemented by `NextScreenResponsePayload` and `FinalScreenResponsePayload`.

* **`NextScreenResponsePayload.java`**

  ```java
  class NextScreenResponsePayload implements FlowResponsePayload {
      String screen;              // Next screen name
      Map<String,Object> data;    // Data needed for that screen (e.g. prompt, options, etc.)
  }
  ```

* **`FinalScreenResponsePayload.java`**

  ```java
  class FinalScreenResponsePayload implements FlowResponsePayload {
      String screen = "SUCCESS";
      ExtensionMessageResponse data;  // Must contain a "flow_token" in params
      static class ExtensionMessageResponse {
          Map<String,Object> params;  // e.g. { flow_token, receipt_id, … }
      }
  }
  ```

* **`FlowStatusRequest.java`**

  ```java
  class FlowStatusRequest {
      String flow_token;
      String status;   // "completed" | "expired" | "failed"
      long timestamp;
      String data;     // Optional Base64 payload
  }
  ```

* **`ErrorNotificationRequest.java`** / **`ErrorNotificationResponse.java`**
  For handling invalid payload notifications from Meta.

  ```java
  class ErrorNotificationRequest {
      String version;
      String flow_token;
      String action;            // "data_exchange" | "INIT"
      Map<String,Object> data;  // { error_key, error_message }
  }
  class ErrorNotificationResponse {
      Acknowledgement data;  // { acknowledged: true }
  }
  ```

* **`HealthCheckRequest.java`** / **`HealthCheckResponse.java`**
  For responding to Meta’s periodic pings.

  ```java
  class HealthCheckRequest { String version; String action; }  // action="ping"
  class HealthCheckResponse { HealthStatus data; }            // { data: { status: "active" } }
  ```

* **`BookingState.java`** (model)

  ```java
  class BookingState {
      String step;        // e.g. "choose_destination", "choose_date", "confirm"
      String destination; // e.g. "New York"
      String date;        // e.g. "2025-05-31"
  }
  ```

---

## Security Configuration Details

If you include `spring-boot-starter-security`, by default Spring Boot will:

* Secure *all* endpoints (redirecting browser requests to a login page).
* Create a random in-memory user with a random password.

You can override this behavior in two main ways:

### 1. Disable Security Auto-Configuration

Add to `application.yml`:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration
      - org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration
```

No Java config is needed—every endpoint (including `/webhook`) is open.

### 2. Permit Only `/webhook/**`

Create `WebFluxSecurityConfig.java` under `com.example.whatsappflows.config`:

```java
package com.example.whatsappflows.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

    @Bean
    public SecurityWebFilterChain securitygWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/webhook/**").permitAll()
                .anyExchange().authenticated()
            );
        return http.build();
    }
}
```

* Allows unauthenticated access to all paths under `/webhook` (and `/webhook/flow/**`).
* Requires authentication for any other endpoint.
* Disables CSRF for the JSON API.

---

## Testing Locally with ngrok

1. **Run** the Spring Boot app on port 3000:

   ```bash
   mvn spring-boot:run
   ```
2. **Start** ngrok:

   ```bash
   ngrok http 3000
   ```
3. **Note** the HTTPS forwarding URL, e.g.,

   ```
   https://abcdef1234.ngrok.io
   ```
4. **In Meta Developer Portal**, set:

    * **Webhook Callback URL** → `https://abcdef1234.ngrok.io/webhook` (verify token: `zap_verify`)
    * **Encrypted Flow Data-Exchange URL** → `https://abcdef1234.ngrok.io/webhook/flow/data-exchange`
5. **Test** by sending a message in WhatsApp to your business account:

    * If you type `"Book Ticket"`, the app will send an unencrypted Flow prompt.
    * If you have already created an encrypted, endpoint-powered Flow, start it via a template or direct invocation in WhatsApp to trigger encrypted data-exchange calls.

---

## Example Flow Lifecycle

### Unencrypted (Builder-Only) Flow

1. **User** sends `"Book Ticket"` in WhatsApp.
2. **Cloud API** → POST `/webhook` with:

   ```json
   {
     "messages": [
       {
         "type": "text",
         "text": { "body": "Book Ticket" },
         "from": "237679889230",
         ...
       }
     ]
   }
   ```
3. **`MessageHandler`** sees `type="text"`, calls `processIncomingText("237679889230", "Book Ticket")`.
4. **`MessageService`** sends the first Flow prompt as raw interactive JSON:

   ```json
   {
     "messaging_product": "whatsapp",
     "to": "237679889230",
     "type": "interactive",
     "interactive": {
       "type": "flow",
       "body": { "text": "Book your ticket" },
       "action": {
         "name": "flow",
         "parameters": {
           "flow_message_version": "3.0",
           "flow_token": "<RANDOM_TOKEN>",
           "flow_id": "<BUILDER_FLOW_ID>"
         }
       }
     }
   }
   ```
5. **WhatsApp client** shows “Book your ticket” Flow UI. User selects “New York.”
6. **Cloud API** → POST `/webhook` with:

   ```json
   {
     "messages": [
       {
         "interactive": {
           "type": "nfm_reply",
           "nfm_reply": {
             "name": "flow",
             "body": "Sent",
             "response_json": "{\"version\":\"3.0\",\"action\":\"data_exchange\",\"screen\":\"choose_destination\",\"data\":{\"screen_0_Name_0\":\"New York\"},\"flow_token\":\"<TOKEN>\"}"
           }
         },
         "from": "237679889230",
         ...
       }
     ]
   }
   ```
7. **`MessageHandler`** sees `interactive.type="nfm_reply"`, JSON has `version/action`, so calls:

   ```java
   flowService.handlePlainExchange(midRequest)
     .flatMap(nextMap -> messageService.sendRawFlowMessage(from, nextMap))
     .subscribe();
   ```

   That sends the next screen (“Select travel date”).
8. **Repeat** until final “Confirm” screen. When Flow is complete, `nfm_reply.response_json` has no `"version"`/`"action"`, so `handlePlainCompletion(...)` is called.
9. **`handlePlainCompletion(...)`** logs final params, sends a confirmation text:

   ```
   ✅ Booking confirmed! Details: date=2025-05-31 destination=New York
   ```

### Encrypted (Endpoint-Powered) Flow

1. **User** taps a template button (or an “interactive flow” message) that starts the encrypted Flow.
2. **WhatsApp client** calls `POST /webhook/flow/data-exchange` with:

   ```json
   {
     "encrypted_flow_data": "<Base64_AES-GCM>",
     "encrypted_aes_key": "<Base64_RSA-OAEP>",
     "initial_vector": "<Base64_IV>"
   }
   ```
3. **`FlowEndpointController.handleDataExchange(...)`** routes to `flowService.handleExchange(...)`.
4. **`FlowService.handleExchange(...)`** decrypts, parses JSON into `FlowDataExchangePayload`, rebuilds `BookingState`, decides next screen, re-encrypts state, and returns:

   ```json
   {
     "version": "3.0",
     "flow_token": "<TOKEN>",
     "data": "<Base64_NewEncryptedState>",
     "screen": { ... NextScreenResponsePayload ... },
     "close": true  // if it’s the final screen
   }
   ```
5. **WhatsApp client** decrypts and renders the next screen UI. This loop continues for all intermediate screens.
6. **On final screen**, `screen="SUCCESS"`, `close=true`; WhatsApp closes the Flow UI and sends a final “Flow Response Message” to your `/webhook` (Cloud API webhook) as an `nfm_reply` with the `response_json` you defined in your final payload.
7. **`MessageHandler`** sees `interactive.type="nfm_reply"` for the final screen (because even encrypted Flows fire one “final response” via the Cloud API), and calls `handlePlainCompletion(...)`.

---

## References

* WhatsApp Cloud API Documentation:
  [https://developers.facebook.com/docs/whatsapp/cloud-api](https://developers.facebook.com/docs/whatsapp/cloud-api)
* WhatsApp Flows Documentation:
  [https://developers.facebook.com/docs/whatsapp/flows/](https://developers.facebook.com/docs/whatsapp/flows/)
* Flow JSON Reference:
  [https://developers.facebook.com/docs/whatsapp/flows/reference/flowjson](https://developers.facebook.com/docs/whatsapp/flows/reference/flowjson)
* Response Message Webhook (`nfm_reply`):
  [https://developers.facebook.com/docs/whatsapp/flows/reference/responsemsgwebhook](https://developers.facebook.com/docs/whatsapp/flows/reference/responsemsgwebhook)
* Error Notification Request:
  [https://developers.facebook.com/docs/whatsapp/flows/guides/implementingyourflowendpoint#error\_notification\_request](https://developers.facebook.com/docs/whatsapp/flows/guides/implementingyourflowendpoint#error_notification_request)
* Health Check Request:
  [https://developers.facebook.com/docs/whatsapp/flows/guides/implementingyourflowendpoint#health\_check\_request](https://developers.facebook.com/docs/whatsapp/flows/guides/implementingyourflowendpoint#health_check_request)
* Spring WebFlux Security:
  [https://docs.spring.io/spring-security/reference/reactive/configuration/webflux.html](https://docs.spring.io/spring-security/reference/reactive/configuration/webflux.html)
* Bouncy Castle JCE Provider:
  [https://www.bouncycastle.org/java.html](https://www.bouncycastle.org/java.html)

---

Copy, configure, and adapt this project to implement both **encrypted** and **unencrypted** WhatsApp Flows in Spring Boot / WebFlux. All core logic lives in the source files referenced above, while this README provides the overarching architecture, configuration, and usage patterns.
