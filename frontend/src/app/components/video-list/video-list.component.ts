import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video-upload';

@Component({
  selector: 'app-video-list',
  templateUrl: './video-list.component.html',
  styleUrls: ['./video-list.component.css']
})
export class VideoListComponent implements OnInit {
  videos: Video[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private videoService: VideoService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadVideos();
  }

  loadVideos(): void {
    this.loading = true;
    this.videoService.getAllVideos().subscribe({
      next: (videos) => {
        this.videos = videos;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading videos:', error);
        this.errorMessage = 'Failed to load videos.';
        this.loading = false;
      }
    });
  }

  viewVideo(videoId: string): void {
    this.router.navigate(['/videos', videoId]);
  }

  toggleLike(video: Video, event: Event): void {
    event.stopPropagation();
    
    // Check if user is logged in (simple check - you can enhance this later)
    const currentUser = localStorage.getItem('currentUser');
    if (!currentUser) {
      alert('You need to log in to like or comment on videos.');
      return;
    }
    
    this.videoService.toggleLike(video.id).subscribe({
      next: () => {
        video.isLiked = !video.isLiked;
        video.likes += video.isLiked ? 1 : -1;
      },
      error: (error) => {
        console.error('Error toggling like:', error);
      }
    });
  }
}
