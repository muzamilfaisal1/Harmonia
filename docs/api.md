# MusicChat API Documentation

## Overview

This document provides comprehensive API documentation for the MusicChat application, including all endpoints, required authentication, roles, and authorization rules.

---

## Authentication & Authorization

### Authentication Methods
- **Session-based Authentication**: Using Spring Security with form login
- **HTTP Basic**: Supported for API testing

### Roles
- `USER` - Standard authenticated user
- `ADMIN` - Administrative user (future use)
- `ANONYMOUS` - Unauthenticated user

### Authorization Model
- **Public Endpoints**: No authentication required
- **Protected Endpoints**: Require authentication (401 if not authenticated)
- **Resource-Owned Endpoints**: User must own the resource (403 if not owner)

---

## Authentication Endpoints

### POST /api/auth/register
**Description**: Register a new user account  
**Authentication**: None required  
**Authorization**: Public

**Request Body**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "username": "string",
  "email": "string",
  "role": "USER"
}
```

---

### POST /api/auth/login
**Description**: Authenticate user and create session  
**Authentication**: None required  
**Authorization**: Public

**Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```

**Response**: `200 OK`
```json
{
  "message": "Login successful",
  "username": "string"
}
```

---

## Chat Endpoints

### POST /api/chat/messages
**Description**: Send a new chat message  
**Authentication**: **Required** (401 if not authenticated)  
**Authorization**: Authenticated users (`USER` role)

**Request Body**:
```json
{
  "content": "string"
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "sender": "username",
  "text": "string",
  "timestamp": "2025-10-02T10:30:00",
  "previousId": null
}
```

**Error Responses**:
- `401 Unauthorized`: Not authenticated
- `400 Bad Request`: Invalid content (empty or null)

---

### GET /api/chat/messages
**Description**: Retrieve all chat messages (ordered by timestamp)  
**Authentication**: None required  
**Authorization**: Public

**Query Parameters**:
- `limit` (optional): Maximum number of messages to return

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "sender": "username",
    "text": "string",
    "timestamp": "2025-10-02T10:30:00",
    "previousId": null
  }
]
```

---

### GET /api/chat/messages/{id}
**Description**: Retrieve a specific message by ID  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "id": 1,
  "sender": "username",
  "text": "string",
  "timestamp": "2025-10-02T10:30:00"
}
```

**Error Responses**:
- `404 Not Found`: Message does not exist

---

### GET /api/chat/messages/sender/{sender}
**Description**: Retrieve all messages from a specific sender  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "sender": "username",
    "text": "string",
    "timestamp": "2025-10-02T10:30:00"
  }
]
```

---

### GET /api/chat/messages/count
**Description**: Get total message count  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "count": 42
}
```

---

## Audio Track Endpoints

### POST /api/audio/upload
**Description**: Upload a new audio file  
**Authentication**: **Required** (401 if not authenticated)  
**Authorization**: Authenticated users (`USER` role)  
**Resource Ownership**: Track is associated with authenticated user

**Request**: `multipart/form-data`
- `file`: Audio file (required)
- `title`: Track title (required)
- `artist`: Artist name (required)

**Response**: `201 Created`
```json
{
  "id": 1,
  "title": "string",
  "artist": "string",
  "filename": "uuid.mp3",
  "originalFilename": "original.mp3",
  "uploadedAt": "2025-10-02T10:30:00",
  "duration": 180,
  "fileSizeBytes": 5242880,
  "contentType": "audio/mpeg",
  "user": {
    "id": 1,
    "username": "string"
  }
}
```

**Error Responses**:
- `401 Unauthorized`: Not authenticated
- `400 Bad Request`: Missing parameters or invalid file
- `500 Internal Server Error`: File upload failed

---

### GET /api/audio/tracks
**Description**: Retrieve all audio tracks  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "title": "string",
    "artist": "string",
    "uploadedAt": "2025-10-02T10:30:00"
  }
]
```

---

### GET /api/audio/tracks/my
**Description**: Retrieve tracks uploaded by the authenticated user  
**Authentication**: **Required** (401 if not authenticated)  
**Authorization**: Authenticated users (`USER` role)  
**Resource Ownership**: Only returns user's own tracks

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "title": "string",
    "artist": "string",
    "uploadedAt": "2025-10-02T10:30:00"
  }
]
```

**Error Responses**:
- `401 Unauthorized`: Not authenticated

---

### GET /api/audio/tracks/{id}
**Description**: Retrieve a specific track by ID  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "id": 1,
  "title": "string",
  "artist": "string",
  "filename": "uuid.mp3",
  "uploadedAt": "2025-10-02T10:30:00"
}
```

**Error Responses**:
- `404 Not Found`: Track does not exist

---

### DELETE /api/audio/tracks/{id}
**Description**: Delete an audio track  
**Authentication**: **Required** (401 if not authenticated)  
**Authorization**: **Resource Owner Only** (403 if not owner)  
**Resource Ownership**: User can only delete their own tracks

**Response**: `200 OK`
```json
{
  "message": "Track deleted successfully"
}
```

**Error Responses**:
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: User does not own this track
- `404 Not Found`: Track does not exist

**Authorization Check**:
```java
// In AudioController.java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (!track.getUser().getUsername().equals(auth.getName())) {
    return 403 Forbidden
}
```

---

### GET /api/audio/download/{id}
**Description**: Download an audio file  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK` (binary audio data)
- Content-Type: audio/mpeg (or appropriate type)
- Content-Disposition: attachment

**Error Responses**:
- `404 Not Found`: Track or file does not exist

---

### GET /api/audio/search/title?q={query}
**Description**: Search tracks by title  
**Authentication**: None required  
**Authorization**: Public

**Query Parameters**:
- `q`: Search query (required)

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "title": "matching title",
    "artist": "string"
  }
]
```

---

### GET /api/audio/search/artist?q={query}
**Description**: Search tracks by artist  
**Authentication**: None required  
**Authorization**: Public

**Response**: Same as search by title

---

### GET /api/audio/count
**Description**: Get total track count  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "count": 25
}
```

---

## Rating Endpoints

### POST /api/audio/{id}/rate
**Description**: Rate an audio track (1-5 stars)  
**Authentication**: None required (uses userId from request body)  
**Authorization**: Public

**Request Body**:
```json
{
  "userId": 1,
  "rating": 5
}
```

**Response**: `200 OK`
```json
{
  "rating": {
    "id": 1,
    "userId": 1,
    "trackId": 1,
    "ratingValue": 5,
    "createdAt": "2025-10-02T10:30:00"
  },
  "averageRating": 4.5,
  "ratingCount": 10,
  "message": "Track rated successfully"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid rating (not 1-5) or missing parameters
- `404 Not Found`: Track does not exist

---

### GET /api/audio/{id}/ratings
**Description**: Get rating statistics for a track  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "trackId": 1,
  "averageRating": 4.5,
  "ratingCount": 10
}
```

---

## Favourite Endpoints

### POST /api/audio/{id}/favorite
**Description**: Toggle favourite status for a track  
**Authentication**: None required (uses userId from request body)  
**Authorization**: Public

**Request Body**:
```json
{
  "userId": 1
}
```

**Response**: `200 OK`
```json
{
  "isFavourited": true,
  "favouriteCount": 5,
  "message": "Track added to favourites"
}
```

---

### GET /api/audio/{id}/favourites
**Description**: Get favourite count for a track  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "trackId": 1,
  "favouriteCount": 5
}
```

---

### GET /api/audio/favorites?userId={userId}
**Description**: Get user's favourite tracks  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "message": "Favorite tracks retrieved successfully",
  "tracks": [
    {
      "id": 1,
      "title": "string",
      "artist": "string"
    }
  ],
  "count": 1
}
```

---

## Playlist Endpoints

### POST /api/playlists
**Description**: Create a new playlist  
**Authentication**: None required (uses ownerId from request body)  
**Authorization**: Public

**Request Body**:
```json
{
  "ownerId": 1,
  "name": "string",
  "description": "string",
  "trackIds": [1, 2, 3]
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "ownerId": 1,
  "name": "string",
  "description": "string",
  "createdAt": "2025-10-02T10:30:00",
  "tracks": [...]
}
```

---

### GET /api/playlists/{id}
**Description**: Retrieve a playlist by ID  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "id": 1,
  "ownerId": 1,
  "name": "string",
  "description": "string",
  "tracks": [...]
}
```

---

### GET /api/playlists/{id}/owner/{ownerId}
**Description**: Retrieve playlist by ID and verify owner  
**Authentication**: None required  
**Authorization**: Public (but returns 404 if owner doesn't match)

**Response**: `200 OK` or `404 Not Found`

---

### GET /api/playlists/owner/{ownerId}
**Description**: Get all playlists by owner  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "name": "string",
    "ownerId": 1
  }
]
```

---

### GET /api/playlists
**Description**: Get all playlists  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
[...]
```

---

### POST /api/playlists/{id}/tracks
**Description**: Add tracks to a playlist  
**Authentication**: None required  
**Authorization**: Public

**Request Body**:
```json
{
  "trackIds": [4, 5, 6]
}
```

**Response**: `200 OK`
```json
{
  "id": 1,
  "tracks": [...]
}
```

---

### DELETE /api/playlists/{id}/tracks
**Description**: Remove tracks from a playlist  
**Authentication**: None required  
**Authorization**: Public

**Request Body**:
```json
{
  "trackIds": [2]
}
```

**Response**: `200 OK`

---

### PUT /api/playlists/{id}
**Description**: Update playlist information  
**Authentication**: None required  
**Authorization**: Public

**Request Body**:
```json
{
  "name": "Updated name",
  "description": "Updated description"
}
```

**Response**: `200 OK`

---

### DELETE /api/playlists/{id}
**Description**: Delete a playlist  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "message": "Playlist deleted successfully"
}
```

---

### GET /api/playlists/search?q={query}
**Description**: Search playlists by name  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`

---

### GET /api/playlists/count
**Description**: Get total playlist count  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "count": 15
}
```

---

## External Music Service Endpoints

### GET /api/audio/search/external?query={query}
**Description**: Search external music database (Deezer API)  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "message": "Search completed successfully",
  "query": "string",
  "count": 5,
  "results": [
    {
      "title": "string",
      "artist": "string",
      "album": "string",
      "duration": 180
    }
  ]
}
```

---

### GET /api/audio/search/external/cache/stats
**Description**: Get external search cache statistics  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`

---

### DELETE /api/audio/search/external/cache
**Description**: Clear external search cache  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`

---

## Test Results Endpoint

### GET /tests
**Description**: Get latest test execution results  
**Authentication**: None required  
**Authorization**: Public

**Response**: `200 OK`
```json
{
  "lastRun": "2025-10-02T14:25:30.123",
  "totalTests": 88,
  "passed": 88,
  "failed": 0,
  "skipped": 0,
  "status": "PASSED"
}
```

**Status Values**:
- `NOT_RUN`: No tests executed yet
- `PASSED`: All tests passed
- `FAILED`: One or more tests failed

---

## WebSocket Endpoints

### ws://localhost:8080/ws-chat
**Description**: WebSocket endpoint for real-time chat  
**Protocol**: STOMP over WebSocket with SockJS fallback  
**Authentication**: Optional (uses principal if available)

#### Subscribe to Messages
**Destination**: `/topic/messages`  
**Description**: Receive broadcast chat messages

**Message Format**:
```json
{
  "id": 1,
  "sender": "username",
  "text": "string",
  "timestamp": "2025-10-02T10:30:00"
}
```

#### Send Message
**Destination**: `/app/chat.send`  
**Description**: Send a chat message (also persisted to database)

**Request**:
```json
{
  "content": "string"
}
```

#### User Join
**Destination**: `/app/chat.join`  
**Description**: Announce user joining chat

**Response**: System message broadcast to all clients

---

## Security Summary

### Endpoints Requiring Authentication (401 if not authenticated)
- `POST /api/chat/messages` - USER role
- `POST /api/audio/upload` - USER role
- `GET /api/audio/tracks/my` - USER role
- `DELETE /api/audio/tracks/{id}` - USER role + owner check

### Endpoints with Resource Ownership (403 if not owner)
- `DELETE /api/audio/tracks/{id}` - Only owner can delete

### Public Endpoints (No authentication required)
- All GET endpoints except `/api/audio/tracks/my`
- `POST /api/audio/{id}/rate`
- `POST /api/audio/{id}/favorite`
- `POST /api/playlists`
- `GET /tests`
- WebSocket connections

---

## Error Response Format

All error responses follow this format:

```json
{
  "error": "Error type",
  "message": "Detailed error message"
}
```

**Common HTTP Status Codes**:
- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions or not resource owner
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server-side error

---

## Rate Limiting

Currently, no rate limiting is implemented. For production deployment, consider implementing rate limiting on:
- Authentication endpoints
- File upload endpoints
- External API search endpoints

---

## CORS Configuration

CORS is enabled for all endpoints with `@CrossOrigin(origins = "*")`.

For production, update to specific origins:
```java
@CrossOrigin(origins = "https://yourdomain.com")
```

---

## Testing with curl

### Send Chat Message (Authenticated)
```bash
curl -X POST http://localhost:8080/api/chat/messages \
  -H "Content-Type: application/json" \
  -u username:password \
  -d '{"content":"Hello World"}'
```

### Upload Audio (Authenticated)
```bash
curl -X POST http://localhost:8080/api/audio/upload \
  -u username:password \
  -F "file=@song.mp3" \
  -F "title=My Song" \
  -F "artist=My Artist"
```

### Get All Tracks (Public)
```bash
curl http://localhost:8080/api/audio/tracks
```

### Rate a Track (Public)
```bash
curl -X POST http://localhost:8080/api/audio/1/rate \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"rating":5}'
```

---

**Last Updated**: October 2, 2025  
**Version**: 3.0 (Week 11 - Final)  
**Status**: Production Ready

