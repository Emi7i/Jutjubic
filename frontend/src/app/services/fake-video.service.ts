import { Injectable } from '@angular/core';
import { Video, GeographicLocation } from '../models/video-upload';

@Injectable({
  providedIn: 'root'
})
export class FakeVideoService {
  private counter = 1;

  /**
   * Generate a single fake video
   * @param topLeft top-left corner of bounding box
   * @param bottomRight bottom-right corner of bounding box
   */
  generateVideo(
    topLeft: [number, number],
    bottomRight: [number, number]
  ): Video {
    const id = crypto.randomUUID();
    const index = this.counter++;

    const location: [number, number] = [
      this.randomFloat(bottomRight[0], topLeft[0]),
      this.randomFloat(topLeft[1], bottomRight[1])
    ];

    return {
      id,
      title: `TestVideo${index.toString().padStart(3, '0')}`,
      description: 'This is a generated test video.',
      tags: this.randomTags(),
      thumbnailUrl: '',
      videoUrl: 'https://cdn.discordapp.com/attachments/1034517160875266119/1469071785692631142/gato-joia.mp4?ex=69865337&is=698501b7&hm=b35f302e254168e6320ac4730aa7e5953829cc219a3e12a506e43127f6a55057&',
      location: { latitude: location[0], longitude: location[1] }, // adapt to your model if needed
      createdAt: this.randomDate(),
      userId: 'test-user',
      userName: 'Test User',
      likes: this.randomInt(0, 500),
      commentsCount: this.randomInt(0, 100),
      viewsCount: this.randomInt(0, 5000),
      isLiked: false
    };
  }

  /**
   * Generate multiple fake videos
   */
  generateVideos(
    count: number,
    topLeft: [number, number],
    bottomRight: [number, number]
  ): Video[] {
    return Array.from({ length: count }, () =>
      this.generateVideo(topLeft, bottomRight)
    );
  }

  /* ----------------- Helpers ----------------- */

  private randomFloat(min: number, max: number): number {
    return Math.random() * (max - min) + min;
  }

  private randomInt(min: number, max: number): number {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  private randomDate(daysBack = 30): Date {
    const now = Date.now();
    const past = now - daysBack * 24 * 60 * 60 * 1000;
    return new Date(this.randomInt(past, now));
  }

  private randomTags(): string[] {
    const tags = ['test', 'demo', 'angular', 'video', 'map', 'leaflet'];
    return tags.sort(() => 0.5 - Math.random()).slice(0, this.randomInt(1, 3));
  }
}
