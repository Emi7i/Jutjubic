import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { VideoUpload, Video, Comment, UploadProgress } from '../models/video-upload';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class VideoService {
 private apiUrl = environment.apiUrl + '/video-posts';

  constructor(private http: HttpClient) { }

  uploadVideo(videoData: VideoUpload): Observable<UploadProgress> {
    const formData = new FormData();

    // Create VideoPost object for the backend
    const videoPost = {
      title: videoData.title,
      videoDescription: videoData.description,
      tags: videoData.tags,
      location: videoData.location ? JSON.stringify(videoData.location) : null
    };

    formData.append('videoPost', new Blob([JSON.stringify(videoPost)], { type: 'application/json' }));
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

  getAllVideos(): Observable<Video[]> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(response => {
        console.log('Raw backend response:', response);
        const videos = response.data || [];
        // Transform backend response to frontend Video model
        return videos.map((video: any) => ({
          id: video.id.toString(),
          title: video.title,
          description: video.videoDescription,
          tags: Array.isArray(video.tags) ? video.tags : (video.tags ? Object.values(video.tags) : []),
          thumbnailUrl: `${this.apiUrl}/${video.id}/thumbnail`,
          videoUrl: `${this.apiUrl}/${video.id}/video`,
          location: video.location ? this.parseLocation(video.location) : undefined,
          createdAt: new Date(video.createdAt),
          userId: video.userId?.toString() || '',
          userName: video.userName || 'Anonymous',
          likes: video.likesCount || 0,
          commentsCount: video.commentsCount || 0,
          viewsCount: video.viewsCount || 0,
          isLiked: false
        }));
      }),
      catchError(error => {
        console.error('Failed to fetch videos:', error);
        console.error('Error response:', error.error);
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

  getVideoById(id: string): Observable<Video> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(
      map(response => {
        console.log('Raw video response:', response);
        const video = response.data;
        // Transform backend response to frontend Video model
        return {
          id: video.id.toString(),
          title: video.title,
          description: video.videoDescription,
          tags: Array.isArray(video.tags) ? video.tags : (video.tags ? Object.values(video.tags) : []),
          thumbnailUrl: `${this.apiUrl}/${video.id}/thumbnail`,
          videoUrl: `${this.apiUrl}/${video.id}/video`,
          location: video.location ? this.parseLocation(video.location) : undefined,
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
        console.error('Error response:', error.error);
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
    return this.http.get<any>(`${this.apiUrl}/${videoId}/comments`).pipe(
      map(response => {
        console.log('Raw comments response:', response);
        return response.data || [];
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
}
