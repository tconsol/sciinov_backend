# 🖼️ Conference Image Upload Feature - Implementation Guide

## Overview

Conference images can now be uploaded via API. Each conference can have **only one image**. When a new image is uploaded, the previous one is automatically deleted from Google Cloud Storage and replaced.

---

## What's New

### Conference Entity
- ✅ `imageUrl` field (already existed)
- ✅ **NEW** `imageBlobName` field - stores the GCS blob path for deletion

### ConferenceService
- ✅ **NEW** `uploadConferenceImage()` method
  - Uploads image to GCS
  - Auto-deletes previous image if exists
  - Stores URL and blob name in database

### ConferenceController
- ✅ **NEW** `POST /api/conferences/{id}/upload-image` endpoint
  - Accepts multipart form data
  - Validates file type (must be image)
  - Returns image URL and blob name

---

## API Endpoint

### Upload Conference Image

```
POST /api/conferences/{conferenceId}/upload-image
Content-Type: multipart/form-data
Authorization: Bearer <ADMIN_OR_SUPER_ADMIN_TOKEN>
```

**Path Parameters:**
```
{conferenceId} = String (Conference ID from database)
```

**Form Data:**
```
image = File (JPEG, PNG, GIF, WebP, etc.)
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Conference image uploaded successfully",
  "conferenceId": "conf123",
  "conferenceName": "ICSE 2026",
  "imageUrl": "https://storage.googleapis.com/sciinovfiles/conferences/icse-2026/image_1708424268.png?X-Goog-Algorithm=GOOG4-RSA-SHA256&...",
  "imageBlobName": "conferences/icse-2026/image_1708424268.png"
}
```

**Error Response (400 - Empty File):**
```json
{
  "success": false,
  "message": "Image file is empty"
}
```

**Error Response (400 - Not Image):**
```json
{
  "success": false,
  "message": "File must be an image (JPEG, PNG, GIF, WebP, etc.)"
}
```

**Error Response (404 - Conference Not Found):**
```json
{
  "success": false,
  "message": "Conference not found: conf123"
}
```

---

## How It Works

### Step-by-Step Process

1. **Admin/Super Admin submits image upload request**
   ```
   POST /api/conferences/{conferenceId}/upload-image
   [Image file in form data]
   ```

2. **Server validates:**
   - Conference exists ✅
   - File is not empty ✅
   - File is an image ✅

3. **If conference already has an image:**
   - Get the blob name from database
   - Delete from GCS bucket
   - Remove old URL reference

4. **Upload new image:**
   - Upload to GCS
   - Get blob name and signed URL
   - Store both in database

5. **Response sent back:**
   - `imageUrl` - Signed URL for accessing the image
   - `imageBlobName` - Path in GCS bucket (for future deletions)

---

## Frontend Usage Examples

### JavaScript / React (Fetch API)

```javascript
const uploadConferenceImage = async (conferenceId, imageFile) => {
  const formData = new FormData();
  formData.append('image', imageFile); // File input element

  try {
    const response = await fetch(
      `/api/conferences/${conferenceId}/upload-image`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${authToken}` // Your JWT token
        },
        body: formData
      }
    );

    const data = await response.json();

    if (data.success) {
      console.log('Image uploaded:', data.imageUrl);
      // Update UI with the new image URL
      document.getElementById('conferenceImage').src = data.imageUrl;
    } else {
      console.error('Upload failed:', data.message);
      alert(`Error: ${data.message}`);
    }
  } catch (error) {
    console.error('Network error:', error);
    alert('Failed to upload image');
  }
};

// Usage
const fileInput = document.getElementById('imageInput');
uploadConferenceImage('conf123', fileInput.files[0]);
```

### HTML Form

```html
<form id="imageUploadForm">
  <input type="file" id="imageInput" accept="image/*" required />
  <button type="submit">Upload Conference Image</button>
</form>

<script>
document.getElementById('imageUploadForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const conferenceId = 'conf123'; // Get from URL or props
  const imageFile = document.getElementById('imageInput').files[0];
  
  await uploadConferenceImage(conferenceId, imageFile);
});
</script>
```

### React Component Example

```jsx
import { useState } from 'react';

function ConferenceImageUpload({ conferenceId }) {
  const [loading, setLoading] = useState(false);
  const [imageUrl, setImageUrl] = useState(null);
  const [error, setError] = useState(null);

  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setLoading(true);
    setError(null);

    const formData = new FormData();
    formData.append('image', file);

    try {
      const response = await fetch(
        `/api/conferences/${conferenceId}/upload-image`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: formData
        }
      );

      const data = await response.json();

      if (data.success) {
        setImageUrl(data.imageUrl);
      } else {
        setError(data.message);
      }
    } catch (err) {
      setError('Failed to upload image');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <input 
        type="file" 
        accept="image/*" 
        onChange={handleImageUpload}
        disabled={loading}
      />
      {loading && <p>Uploading...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {imageUrl && <img src={imageUrl} alt="Conference" style={{ maxWidth: '300px' }} />}
    </div>
  );
}

export default ConferenceImageUpload;
```

---

## Database Schema

### Conference Collection

```json
{
  "_id": "ObjectId",
  "title": "ICSE 2026",
  "imageUrl": "https://storage.googleapis.com/.../image.png",
  "imageBlobName": "conferences/icse-2026/image_1708424268.png",
  "status": "ACTIVE",
  "dashboardMasterIds": ["dm1", "dm2"],
  "createdAt": "2026-02-20T10:00:00",
  "updatedAt": "2026-02-20T16:20:00",
  "deleted": false
}
```

**New fields:**
- `imageBlobName` - Stores the GCS path (for deletion when updating)

---

## Image Flow Diagram

```
Frontend                    Backend                     Google Cloud Storage
   │                          │                                  │
   │  POST /upload-image       │                                  │
   ├─────────────────────────>│                                  │
   │  (image file)             │                                  │
   │                           │ 1. Validate file                │
   │                           │ 2. Get old blobName             │
   │                           │ 3. Delete old image ────────────┼─────> DELETE
   │                           │                                  │
   │                           │ 4. Upload new image ────────────┼─────> UPLOAD
   │                           │ 5. Get blobName & URL           │  
   │                           │ 6. Save to DB                   │
   │  ← Success + imageUrl     │                                  │
   │<─────────────────────────┤                                  │
   │                           │                                  │
```

---

## File Size Limits

The following limits are set in `.env`:

```properties
MAX_FILE_SIZE=50MB
MAX_REQUEST_SIZE=50MB
```

Adjust in `.env` if needed for different image sizes.

---

## Supported Image Formats

- ✅ JPEG (.jpg, .jpeg)
- ✅ PNG (.png)
- ✅ GIF (.gif)
- ✅ WebP (.webp)
- ✅ BMP (.bmp)
- ✅ TIFF (.tiff)
- ✅ SVG (.svg)

Any file with `Content-Type: image/*` is accepted.

---

## Security Features

1. **File Type Validation** - Only image files are accepted
2. **File Size Limits** - Configurable max size in `.env`
3. **Authentication Required** - Only ADMIN or SUPER_ADMIN can upload
4. **Automatic Cleanup** - Old images deleted from bucket
5. **Unique Filenames** - GCS generates unique names to prevent collisions
6. **Signed URLs** - Image URLs expire after a period (configurable in GoogleCloudStorageService)

---

## Error Handling

| Scenario | Status | Response |
|----------|--------|----------|
| Empty file | 400 | `"Image file is empty"` |
| Not an image | 400 | `"File must be an image..."` |
| Conference not found | 400 | `"Conference not found: {id}"` |
| GCS upload error | 500 | `"Failed to upload image: {error}"` |
| Unexpected error | 500 | `"Unexpected error: {error}"` |

---

## Implementation Checklist

- ✅ Conference entity updated with `imageBlobName` field
- ✅ ConferenceService has `uploadConferenceImage()` method
- ✅ ConferenceController has `/upload-image` endpoint
- ✅ Auto-delete previous image implemented
- ✅ Image URL and blob name stored in database
- ✅ File type validation
- ✅ Error handling
- ✅ Logging

---

## Testing the Endpoint

### Using cURL

```bash
curl -X POST http://localhost:8080/api/conferences/conf123/upload-image \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "image=@/path/to/image.png"
```

### Response Example

```json
{
  "success": true,
  "message": "Conference image uploaded successfully",
  "conferenceId": "conf123",
  "conferenceName": "ICSE 2026",
  "imageUrl": "https://storage.googleapis.com/sciinovfiles/conferences/icse-2026/image_1708424268.png?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=...",
  "imageBlobName": "conferences/icse-2026/image_1708424268.png"
}
```

---

## Next Steps

1. ✅ API is ready
2. 📝 Create UI form for image upload in your frontend
3. 🧪 Test with different image formats and sizes
4. 🚀 Deploy to production

