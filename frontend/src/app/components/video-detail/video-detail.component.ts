import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video, Comment } from '../../models/video-upload';

@Component({
  selector: 'app-video-detail',
  templateUrl: './video-detail.component.html',
  styleUrls: ['./video-detail.component.css']
})
export class VideoDetailComponent implements OnInit {
  video: Video | null = null;
  comments: Comment[] = [];
  newComment = '';
  loading = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService
  ) { }

  ngOnInit(): void {
    const videoId = this.route.snapshot.paramMap.get('id');
    if (videoId) {
      this.loadVideo(videoId);
      this.loadComments(videoId);
    }
  }

  loadVideo(videoId: string): void {
    this.loading = true;
    this.videoService.getVideoById(videoId).subscribe({
      next: (video) => {
        this.video = video;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading video:', error);
        this.errorMessage = 'Failed to load video.';
        this.loading = false;
      }
    });
  }

  loadComments(videoId: string): void {
    this.videoService.getComments(videoId).subscribe({
      next: (comments) => {
        this.comments = comments;
      },
      error: (error) => {
        console.error('Error loading comments:', error);
      }
    });
  }

  toggleLike(): void {
    if (!this.video) return;

    // Check if user is logged in
    const currentUser = localStorage.getItem('currentUser');
    if (!currentUser) {
      alert('You need to log in to like or comment on videos.');
      return;
    }

    this.videoService.toggleLike(this.video.id).subscribe({
      next: () => {
        if (this.video) {
          this.video.isLiked = !this.video.isLiked;
          this.video.likes += this.video.isLiked ? 1 : -1;
        }
      },
      error: (error) => {
        console.error('Error toggling like:', error);
      }
    });
  }

  addComment(): void {
    if (!this.video || !this.newComment.trim()) return;

    // Check if user is logged in
    const currentUser = localStorage.getItem('currentUser');
    if (!currentUser) {
      alert('You need to log in to like or comment on videos.');
      return;
    }

    this.videoService.addComment(this.video.id, this.newComment).subscribe({
      next: (comment) => {
        this.comments.unshift(comment);
        this.newComment = '';
        if (this.video) {
          this.video.commentsCount++;
        }
      },
      error: (error) => {
        console.error('Error adding comment:', error);
        this.errorMessage = 'Failed to add comment.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/videos']);
  }
}
