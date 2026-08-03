# Surword - Web Text Based Game | Backend

This Spring Boot backend powers the Surword text-based RPG by handling game state, player progression, combat, inventory, and backend API logic. The frontend is implemented in a separate repository:

- https://github.com/Darosss/Surword-WebTextBasedGame-FE

## The backend is responsible for:

- user authentication and JWT-based security
- game persistence with MongoDB
- turn-based combat and battle reports
- character creation, mercenary management, and item equipment
- skirmishes, dungeons, and loot handling
- integration with a frontend through REST APIs and Swagger

## Built with

- Java 17
- Spring Boot 3.2
- Spring Web
- Spring Security
- Spring Data MongoDB
- Morphia for MongoDB mapping
- jjwt for JWT authentication
- springdoc-openapi for Swagger UI
- Gradle

## Key features

- Email/password registration and JWT login
- Secure API endpoints for authenticated gameplay
- Main character creation, mercenary creation, and character management
- Consumable and wearable item handling with equipment slots
- Skirmishes and dungeon fights with reward processing
- Leaderboards, merchant shop actions, and inventory endpoints
- Centralized error handling and debug endpoints for development

## Architecture overview

The repository is organized into backend modules such as:

- `auth` - authentication, JWT generation, security configuration
- `users` - user profile, token info, user management
- `characters` - character creation, equip/un-equip, consumables
- `items` - item service, inventory handling, item generation
- `skirmishes` - challenge management, dungeon fights, async reward application
- `merchants` - buy/sell item flow and merchant inventories
- `leaderboards` - ranking and refresh scheduling
- `settings` - game configuration, limits, and balance values
- `battle` - combat engine, fight reports, attack/defense calculations

## API and integration

Swagger UI is available after startup at:

- `http://localhost:8080/swagger-ui/index.html`

Important endpoint groups:

- `/auth/login`, `/auth/register`
- `/users`, `/users/profile`, `/users/token-info`
- `/characters/*` for character and equipment actions
- `/skirmishes/*` for skirmish and dungeon gameplay
- `/merchants/*` for buy/sell actions
- `/leaderboards/*` for ranking data

> Note: paths under `/admin/**` require admin role, and most other routes require an authenticated JWT token.

## Getting started

1. Install Java 17.
2. Configure environment variables in a `.env` file or your environment.
3. Run the backend:

```bash
./gradlew bootRun
```

or use your Java IDE to run `TextBasedGameApplication`.

Once running, open Swagger at `http://localhost:8080/swagger-ui/index.html`.

## Environment variables

This application loads runtime settings from `.env` via `java-dotenv`.

Required values include:

- `CORS_ORIGINS` - allowed frontend origins for CORS
- `MONGODB_URI` - connection URI for MongoDB
- `MONGODB_NAME` - target MongoDB database name
- `JWT_SECRET_KEY` - secret key for JWT signing
- `JWT_TOKEN_VALIDITY_MS` - JWT token validity in milliseconds

## Future improvements

- add full integration tests for API flows
- support additional item types and enemy AI
- implement multiplayer sessions or real-time updates
- improve admin tooling and deployment scripts

## Contact

Darosss - darosss12@proton.me
