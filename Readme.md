# Hi Town Calculator Bot

A calculator bot for Hi Town that can handle a wide range of mathematical computations.

## Features

- Basic arithmetic operations (+, -, \*, /, ^)
- Mathematical functions (sin, cos, tan, sqrt, log, ln)
- Constants (pi, e)
- Parentheses and nested expressions
- Error handling and validation
- State persistence
- Webhook support

## Installation

1. Clone the repository
2. Copy `.env.example` to `.env` and configure the environment variables
3. Run `./gradlew build` to build the project

## Running

### Development

```bash
./gradlew run
```

### Production

```bash
java -jar build/libs/hi-town-calculator-bot-0.0.1.jar
```

## Deployment on Render

1. Create a new Web Service on Render
2. Connect your GitHub repository
3. Configure the following settings:

   - Name: `hi-town-calculator-bot` (or your preferred name)
   - Environment: `Docker`
   - Branch: `main` (or your deployment branch)
   - Root Directory: `/` (default)
   - Instance Type: `Free` (or your preferred plan)

4. Add the following environment variables:

   ```
   PORT=8080
   BOT_SECRET=your-secret-here
   BOT_STATE_FILE=/app/bot_state.json
   ```

5. Click "Create Web Service"

The service will automatically build and deploy your bot. Render will provide you with a URL where your bot is accessible.

## API Documentation

### GET /

Returns bot information.

### POST /install

Installs the bot in a group.

### POST /reinstall

Updates bot configuration.

### POST /uninstall

Removes bot from a group.

### POST /message

Processes messages from the group.

### POST /pause

Pauses bot in a group.

### POST /resume

Resumes bot in a group.

## Calculator Usage

Use `@calc` followed by your expression:

```
@calc 2 + 2
@calc sin(45)
@calc sqrt(16) + log(100)
@calc pi * e
```

## Monitoring

- Health check: GET /health
- Metrics: GET /metrics
- Logs: Check application logs in /logs directory

## Security

- All endpoints except GET / require authentication
- Rate limiting is enabled
- CORS is configured for Hi Town domains
- Input validation is implemented
- Secrets are managed via environment variables

## Error Handling

The bot provides clear error messages for:

- Invalid expressions
- Division by zero
- Invalid number formats
- Mismatched parentheses
- Unsupported operations

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request
