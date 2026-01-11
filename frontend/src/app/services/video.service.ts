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

    formData.append('title', videoData.title);
    formData.append('description', videoData.description);
    formData.append('tags', JSON.stringify(videoData.tags));
    formData.append('thumbnail', videoData.thumbnail, videoData.thumbnail.name);
    formData.append('video', videoData.video, videoData.video.name);

    if (videoData.location) {
      formData.append('location', JSON.stringify(videoData.location));
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
      map(response => response.data || []),
      catchError(error => {
        console.error('Failed to fetch videos:', error);
        return throwError(() => error);
      })
    );
  }

  getVideoById(id: string): Observable<Video> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.data),
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
    return this.http.post<Comment>(`${this.apiUrl}/${videoId}/comments`, { text });
  }

  getComments(videoId: string): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.apiUrl}/${videoId}/comments`);
  }

  deleteVideo(videoId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${videoId}`);
  }
}
