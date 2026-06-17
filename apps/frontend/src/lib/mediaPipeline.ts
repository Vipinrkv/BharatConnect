/**
 * Frontend Media Pipeline: Resize, Compress, Convert WebP, Encrypt (AES-GCM), and Checksum
 */

export interface ProcessedMedia {
  encryptedData: ArrayBuffer;
  rawKey: ArrayBuffer; // Raw key bytes to be stored in E2EE key ring
  iv: Uint8Array;
  checksum: string;
  size: number;
}

/**
 * Resizes an image file and exports it as a WebP blob.
 */
export async function resizeAndConvertToWebP(
  file: File,
  targetWidth: number,
  targetHeight: number,
  preserveRatio: boolean = true
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.src = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(img.src);
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        reject(new Error('Failed to get 2D canvas context'));
        return;
      }

      let width = targetWidth;
      let height = targetHeight;

      if (preserveRatio) {
        const ratio = Math.min(targetWidth / img.width, targetHeight / img.height);
        width = img.width * ratio;
        height = img.height * ratio;
      }

      canvas.width = width;
      canvas.height = height;

      // Draw and resize image onto canvas
      ctx.drawImage(img, 0, 0, width, height);

      // Export as WebP
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob);
          } else {
            reject(new Error('Canvas to WebP blob conversion failed'));
          }
        },
        'image/webp',
        0.8 // WebP compression quality
      );
    };
    img.onerror = () => reject(new Error('Failed to load image file'));
  });
}

/**
 * Encrypts an ArrayBuffer payload using AES-256-GCM.
 */
export async function encryptMedia(data: ArrayBuffer): Promise<{
  encryptedData: ArrayBuffer;
  rawKey: ArrayBuffer;
  iv: Uint8Array;
}> {
  // 1. Generate AES-GCM 256-bit Key
  const key = await window.crypto.subtle.generateKey(
    {
      name: 'AES-GCM',
      length: 256,
    },
    true,
    ['encrypt', 'decrypt']
  );

  // 2. Generate random 12-byte IV (Initialization Vector)
  const iv = window.crypto.getRandomValues(new Uint8Array(12));

  // 3. Encrypt data
  const encryptedData = await window.crypto.subtle.encrypt(
    {
      name: 'AES-GCM',
      iv: iv,
    },
    key,
    data
  );

  // 4. Export key bytes
  const rawKey = await window.crypto.subtle.exportKey('raw', key);

  return {
    encryptedData,
    rawKey,
    iv,
  };
}

/**
 * Calculates SHA-256 checksum of an ArrayBuffer.
 */
export async function calculateSHA256(data: ArrayBuffer): Promise<string> {
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Complete media processing flow depending on module policies (avatar, banner, chat-media).
 */
export async function processMediaPipeline(
  file: File,
  type: 'avatar' | 'banner' | 'chat-media' | 'document'
): Promise<ProcessedMedia> {
  let processedBlob: Blob | File = file;

  // 1. Run resizing and WebP conversion for images
  if (file.type.startsWith('image/')) {
    if (type === 'avatar') {
      // Limit avatar dimensions to 512x512
      processedBlob = await resizeAndConvertToWebP(file, 512, 512, false);
    } else if (type === 'banner') {
      // Limit banner dimensions to 1920x1080
      processedBlob = await resizeAndConvertToWebP(file, 1920, 1080, true);
    } else if (type === 'chat-media') {
      // Standard message images resized to maximum 1280px bounding box
      processedBlob = await resizeAndConvertToWebP(file, 1280, 1280, true);
    }
  }

  // Convert Blob to ArrayBuffer
  const buffer = await processedBlob.arrayBuffer();

  // 2. Encrypt
  const { encryptedData, rawKey, iv } = await encryptMedia(buffer);

  // 3. Checksum
  const checksum = await calculateSHA256(encryptedData);

  return {
    encryptedData,
    rawKey,
    iv,
    checksum,
    size: encryptedData.byteLength,
  };
}

/**
 * Decrypts an encrypted ArrayBuffer payload.
 */
export async function decryptMedia(
  encryptedData: ArrayBuffer,
  rawKey: ArrayBuffer,
  iv: Uint8Array
): Promise<ArrayBuffer> {
  // Import raw key bytes
  const key = await window.crypto.subtle.importKey(
    'raw',
    rawKey,
    'AES-GCM',
    true,
    ['decrypt']
  );

  // Decrypt data
  return await window.crypto.subtle.decrypt(
    {
      name: 'AES-GCM',
      iv: iv as any,
    },
    key,
    encryptedData
  );
}
