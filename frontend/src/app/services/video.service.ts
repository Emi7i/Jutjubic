import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { VideoUpload, Video, Comment, UploadProgress, GeographicLocation } from '../models/video-upload';
import { environment } from 'src/environments/environment';

// for testing purposes
import { FakeVideoService } from './fake-video.service';

@Injectable({
  providedIn: 'root'
})
export class VideoService {
 private apiUrl = environment.apiUrl + '/video-posts';

  constructor(private http: HttpClient, private fakeVideoService: FakeVideoService) { }

  uploadVideo(videoData: VideoUpload): Observable<UploadProgress> {
    const formData = new FormData();

    // Create VideoPost object for the backend
    const videoPost = {
      title: videoData.title,
      videoDescription: videoData.description,
      tags: videoData.tags,
      longitude: videoData.location?.longitude,
      latitude: videoData.location?.latitude

    };

    formData.append('videos', new Blob([JSON.stringify(videoPost)], { type: 'application/json' }));
    formData.append('videoFile', videoData.video, videoData.video.name);

    if (videoData.thumbnail) {
      formData.append('thumbnailFile', videoData.thumbnail, videoData.thumbnail.name);
    }

    return this.http.post<any>(`${this.apiUrl}/upload`, formData, {
      reportProgress: true,
      observe: 'events'
    }).pipe(
      map((event: HttpEvent<any>) => {
        return this.getEventProgress(event);
      }),
      catchError(error => {
        console.error('Upload error:', error);
        return throwError(() => error);
      })
    );
  }

  private getEventProgress(event: HttpEvent<any>): UploadProgress {
    switch (event.type) {
      case HttpEventType.Sent:
        return { percentage: 0, status: 'uploading', message: 'Upload started' };

      case HttpEventType.UploadProgress:
        const percentDone = event.total ? Math.round(100 * event.loaded / event.total) : 0;
        return {
          percentage: percentDone,
          status: 'uploading',
          message: `Uploading: ${percentDone}%`
        };

      case HttpEventType.Response:
        return {
          percentage: 100,
          status: 'complete',
          message: 'Upload complete'
        };

      default:
        return { percentage: 0, status: 'pending' };
    }
  }

  // --- All videos ---
  getAllVideos(): Observable<Video[]> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(response => {
        const videos = response.data || [];
        return videos.map((video: any) => {
          const transformed: Video = {
            id: video.id.toString(),
            title: video.title,
            description: video.videoDescription,
            tags: Array.isArray(video.tags) ? video.tags : (video.tags ? Object.values(video.tags) : []),
            thumbnailUrl: `${this.apiUrl}/${video.id}/thumbnail`,
            videoUrl: `${this.apiUrl}/${video.id}/video`,
            location: this.generateGeographicLocation(video),
            createdAt: new Date(video.createdAt),
            userId: video.userId?.toString() || '',
            userName: video.userName || 'Anonymous',
            likes: video.likesCount || 0,
            commentsCount: video.commentsCount || 0,
            viewsCount: video.viewsCount || 0,
            isLiked: false
          };
          return transformed;
        });
      }),
      catchError(error => {
        console.error('Failed to fetch videos:', error);
        return throwError(() => error);
      })
    );
  }

  private parseLocation(locationString: string): any {
    try {
      // If it's already JSON, parse it
      if (locationString.startsWith('{')) {
        return JSON.parse(locationString);
      }
      // If it's plain text, return as address string
      return {
        address: locationString,
        latitude: null,
        longitude: null
      };
    } catch (e) {
      console.warn('Failed to parse location:', locationString);
      return {
        address: locationString,
        latitude: null,
        longitude: null
      };
    }
  }

  // --- Single video ---
  getVideoById(id: string): Observable<Video> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(
      map(response => {
        const video = response.data;
        return {
          id: video.id.toString(),
          title: video.title,
          description: video.videoDescription,
          tags: Array.isArray(video.tags) ? video.tags : (video.tags ? Object.values(video.tags) : []),
          thumbnailUrl: `${this.apiUrl}/${video.id}/thumbnail`,
          videoUrl: `${this.apiUrl}/${video.id}/video`,
          location: this.generateGeographicLocation(video),
          createdAt: new Date(video.createdAt),
          userId: video.userId?.toString() || '',
          userName: video.userName || 'Anonymous',
          likes: video.likesCount || 0,
          commentsCount: video.commentsCount || 0,
          viewsCount: video.viewsCount || 0,
          isLiked: false
        };
      }),
      catchError(error => {
        console.error('Failed to fetch video:', error);
        return throwError(() => error);
      })
    );
  }

  toggleLike(videoId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/${videoId}/like`, {});
  }

  addComment(videoId: string, text: string): Observable<Comment> {
    return this.http.post<any>(`${this.apiUrl}/${videoId}/comments`, { text }).pipe(
      map(response => {
        console.log('Raw add comment response:', response);
        return response.data;
      }),
      catchError(error => {
        console.error('Failed to add comment:', error);
        return throwError(() => error);
      })
    );
  }

  getComments(videoId: string): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.apiUrl}/${videoId}/comments`).pipe(
      map(comments => {
        // comments is already an array, just return it
        return comments.map(comment => ({
          ...comment,
          // optional: parse createdAt to Date if needed
          createdAt: new Date(comment.createdAt)
        }));
      }),
      catchError(error => {
        console.error('Failed to fetch comments:', error);
        return [];
      })
    );
  }




  deleteVideo(videoId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${videoId}`);
  }

  getVideosInBoundingBox(
    minLon: number,
    minLat: number,
    maxLon: number,
    maxLat: number,
    from?: Date,
    to?: Date
  ): Observable<Video[]> {
    let params = new HttpParams()
      .set('minLon', minLon)
      .set('minLat', minLat)
      .set('maxLon', maxLon)
      .set('maxLat', maxLat);

    if (from) params = params.set('from', from.toISOString());
    if (to) params = params.set('to', to.toISOString());

    return this.http.get<any>(`${environment.apiUrl}/tiles/videos`, { params }).pipe(
      map(response => {
        // Check if response is array or object
        const videosArray: any[] = Array.isArray(response) ? response : response.data || [];

        return videosArray.map(video => ({
          id: video.id.toString(),
          title: video.title,
          description: video.videoDescription,
          tags: Array.isArray(video.tags) ? video.tags : (video.tags ? Object.values(video.tags) : []),
          thumbnailUrl: `${this.apiUrl}/${video.id}/thumbnail`,
          videoUrl: `${this.apiUrl}/${video.id}/video`,
          location: this.generateGeographicLocation(video),
          createdAt: new Date(video.createdAt),
          userId: video.userId?.toString() || '',
          userName: video.userName || 'Anonymous',
          likes: video.likesCount || 0,
          commentsCount: video.commentsCount || 0,
          viewsCount: video.viewsCount || 0,
          isLiked: false
        }));
      }),
      catchError(err => {
        console.error('Failed to fetch bounding box videos', err);
        return [];
      })
    );
  }


  getVideosInBoundingBoxTuples(
    sw: [number, number],
    ne: [number, number],
    from?: Date,
    to?: Date,
    round = true
  ): Observable<Video[]> {
    const minLat = round ? Math.floor(sw[0]) : sw[0];
    const minLon = round ? Math.floor(sw[1]) : sw[1];
    const maxLat = round ? Math.ceil(ne[0]) : ne[0];
    const maxLon = round ? Math.ceil(ne[1]) : ne[1];

    return this.getVideosInBoundingBox(minLon, minLat, maxLon, maxLat, from, to);
  }

  // --- Generate location object ---
  private generateGeographicLocation(video: any): GeographicLocation {
    return {
      latitude: video.latitude ?? null,
      longitude: video.longitude ?? null,
      address: video.location ?? ''
    };
  }

  // --- Get Popular Tags ---
  getPopularTags(): Observable<string[]> {
    return this.http.get<any>(`${this.apiUrl}/tags/popular`).pipe(
      map(response => {
        // Handle different response formats
        let tags: string[] = [];
        
        if (Array.isArray(response)) {
          tags = response;
        } else if (response.data && Array.isArray(response.data)) {
          tags = response.data;
        } else {
          // Fallback: extract tags from all videos
          tags = this.extractTagsFromVideos();
        }
        
        return tags;
      }),
      catchError(error => {
        console.error('Failed to fetch popular tags:', error);
        // Return fallback tags as observable
        return of(['music', 'gaming', 'education', 'sports', 'comedy', 'entertainment', 'news', 'technology', 'tutorial', 'vlog']);
      })
    );
  }

  // Fallback method to extract tags from existing videos
  private extractTagsFromVideos(): string[] {
    // This would require caching videos or making another call
    // For now, return common tags as fallback
    return ['music', 'gaming', 'education', 'sports', 'comedy', 'entertainment', 'news', 'technology', 'tutorial', 'vlog'];
  }
}
