import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video-upload';

// Extended interface to include optional properties
interface ExtendedVideo extends Video {
  duration?: number;
  userAvatar?: string;
}

@Component({
  selector: 'app-video-list',
  templateUrl: './video-list.component.html',
  styleUrls: ['./video-list.component.css']
})
export class VideoListComponent implements OnInit {
  videos: ExtendedVideo[] = [];
  filteredVideos: ExtendedVideo[] = [];
  popularTags: string[] = [];
  selectedTag: string = '';
  loading = false;
  errorMessage = '';

  constructor(
    private videoService: VideoService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadVideos();
    this.loadPopularTags();
  }

  loadVideos(): void {
    this.loading = true;
    this.videoService.getAllVideos().subscribe({
      next: (videos) => {
        this.videos = videos as ExtendedVideo[];
        this.filteredVideos = this.videos;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading videos:', error);
        this.errorMessage = 'Failed to load videos.';
        this.loading = false;
      }
    });
  }

  loadPopularTags(): void {
    this.videoService.getPopularTags().subscribe({
      next: (tags) => {
        this.popularTags = tags.slice(0, 10); // Get top 10 tags
      },
      error: (error) => {
        console.error('Error loading popular tags:', error);
        // Fallback to some default tags if API fails
        this.popularTags = ['music', 'gaming', 'education', 'sports', 'comedy', 'entertainment', 'news', 'technology', 'tutorial', 'vlog'];
      }
    });
  }

  filterByTag(tag: string): void {
    this.selectedTag = tag;
    
    if (tag === '') {
      // Show all videos
      this.filteredVideos = this.videos;
    } else {
      // Filter videos by selected tag
      this.filteredVideos = this.videos.filter(video => 
        video.tags && video.tags.includes(tag)
      );
    }
    
    // Update active state for tag buttons
    this.updateActiveTagButton();
  }

  updateActiveTagButton(): void {
    // This will be handled in the template with [class.active]
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

  formatDuration(duration?: number): string {
    if (!duration) return '0:00';
    
    const minutes = Math.floor(duration / 60);
    const seconds = Math.floor(duration % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  formatViews(views?: number): string {
    if (!views) return '0';
    
    if (views >= 1000000) {
      return (views / 1000000).toFixed(1) + 'M';
    } else if (views >= 1000) {
      return (views / 1000).toFixed(1) + 'K';
    }
    return views.toString();
  }

  formatDate(date?: Date | string): string {
    if (!date) return 'No date';
    
    const videoDate = typeof date === 'string' ? new Date(date) : date;
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - videoDate.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) {
      return '1 day ago';
    } else if (diffDays < 7) {
      return `${diffDays} days ago`;
    } else if (diffDays < 30) {
      const weeks = Math.floor(diffDays / 7);
      return `${weeks} week${weeks > 1 ? 's' : ''} ago`;
    } else if (diffDays < 365) {
      const months = Math.floor(diffDays / 30);
      return `${months} month${months > 1 ? 's' : ''} ago`;
    } else {
      const years = Math.floor(diffDays / 365);
      return `${years} year${years > 1 ? 's' : ''} ago`;
    }
  }
}
