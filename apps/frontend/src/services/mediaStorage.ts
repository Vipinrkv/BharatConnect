import { supabase } from '../config/supabase';
import { processMediaPipeline, decryptMedia, ProcessedMedia } from '../lib/mediaPipeline';

export class MediaStorageService {
  /**
   * Helper to convert Uint8Array IV to Hex string for database storage
   */
  private bytesToHex(bytes: Uint8Array): string {
    return Array.from(bytes)
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  }

  /**
   * Helper to convert Hex string back to Uint8Array IV
   */
  private hexToBytes(hex: string): Uint8Array {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
      bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
    }
    return bytes;
  }

  /**
   * Process and upload encrypted file to Supabase Storage
   * Returns metadata for database persistence
   */
  async uploadMedia(
    file: File,
    bucket: 'avatars' | 'banners' | 'chat-media' | 'voice-notes' | 'documents',
    folderId: string, // user_id or chat_id
    fileName: string
  ): Promise<{
    url: string;
    size: number;
    checksum: string;
    keyReference: string; // Serialized IV + Key bytes for E2EE storage
  }> {
    // 1. Process media through pipeline (resize, convert WebP, compress, encrypt)
    const pipelineType = bucket === 'avatars' ? 'avatar' : bucket === 'banners' ? 'banner' : 'chat-media';
    const processed: ProcessedMedia = await processMediaPipeline(file, pipelineType);

    // 2. Upload ciphertext binary array to Supabase Storage
    const filePath = `${folderId}/${fileName}`;
    const fileBlob = new Blob([processed.encryptedData], { type: 'application/octet-stream' });

    const { error } = await supabase.storage
      .from(bucket)
      .upload(filePath, fileBlob, {
        contentType: 'application/octet-stream',
        upsert: true,
      });

    if (error) throw error;

    // Resolve public URL for public buckets, or private path for private buckets
    const { data: urlData } = supabase.storage.from(bucket).getPublicUrl(filePath);
    const mediaUrl = urlData?.publicUrl || filePath;

    // Serialize key bytes & IV to reference (In production, this key should be encrypted with receiver's public key)
    const serializedKeyInfo = JSON.stringify({
      rawKeyHex: this.bytesToHex(new Uint8Array(processed.rawKey)),
      ivHex: this.bytesToHex(processed.iv),
    });

    return {
      url: mediaUrl,
      size: processed.size,
      checksum: processed.checksum,
      keyReference: serializedKeyInfo,
    };
  }

  /**
   * Download and decrypt E2EE media file
   * Returns a local object URL to display in <img> or <video> tags
   */
  async downloadAndDecryptMedia(
    bucket: 'chat-media' | 'voice-notes' | 'documents',
    filePath: string,
    keyReference: string,
    mimeType: string = 'image/webp'
  ): Promise<string> {
    // 1. Parse Key references
    const keyInfo = JSON.parse(keyReference);
    const rawKey = this.hexToBytes(keyInfo.rawKeyHex).buffer as ArrayBuffer;
    const iv = this.hexToBytes(keyInfo.ivHex);

    // 2. Download encrypted payload from storage
    const { data, error } = await supabase.storage.from(bucket).download(filePath);
    if (error) throw error;

    const encryptedArrayBuffer = await data.arrayBuffer();

    // 3. Decrypt payload
    const decryptedArrayBuffer = await decryptMedia(encryptedArrayBuffer, rawKey, iv);

    // 4. Create local blob URL
    const decryptedBlob = new Blob([decryptedArrayBuffer], { type: mimeType });
    return URL.createObjectURL(decryptedBlob);
  }
}

export const mediaStorageService = new MediaStorageService();
