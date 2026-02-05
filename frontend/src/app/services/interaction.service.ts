import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { Video, Comment } from '../models/video-upload';
import { VideoService } from './video.service';
import { catchError, map, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class InteractionService {

  constructor(private videoService: VideoService) { }

  /** Toggle like for a video by ID */
  toggleLike(videoId: string): Observable<Video> {
    const currentUser = localStorage.getItem('currentUser');
    if (!currentUser) {
      return throwError(() => new Error('User not logged in'));
    }

    return this.videoService.toggleLike(videoId).pipe(
      catchError(err => {
        console.error('Error toggling like:', err);
        throw err;
      })
    );
  }

  /** Add a comment to a video by ID */
  addComment(videoId: string, text: string): Observable<Comment> {
    const currentUser = localStorage.getItem('currentUser');
    if (!currentUser) {
      return throwError(() => new Error('User not logged in'));
    }

    if (!text.trim()) {
      return throwError(() => new Error('Comment text is empty'));
    }

    return this.videoService.addComment(videoId, text).pipe(
      catchError(err => {
        console.error('Error adding comment:', err);
        throw err;
      })
    );
  }

  /** Load comments for a video by ID */
  loadComments(videoId: string): Observable<Comment[]> {
    return this.videoService.getComments(videoId).pipe(
      catchError(err => {
        console.error('Error loading comments:', err);
        return of([]); // fallback to empty array
      })
    );
  }
}
