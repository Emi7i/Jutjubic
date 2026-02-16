import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video-upload';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  title = 'Jutjubić';
  isLoggedIn = false;
  popularVideos: Video[] = [];
  currentIndex: number = 0;
  animating: boolean = false;

  constructor(private router: Router, private videoService: VideoService) {}

  ngOnInit(): void {
    this.checkLoginStatus();
    this.loadPopularVideos();
  }

  private checkLoginStatus(): void {
    // Check if user is logged in by checking for auth token
    const token = localStorage.getItem('authToken');
    this.isLoggedIn = !!token;
  }

  private loadPopularVideos(): void {
    this.videoService.getTopPopularVideos().subscribe({
      next: (videos: Video[]) => {
        this.popularVideos = videos;
        this.currentIndex = 0;
      },
      error: (error: any) => {
        console.error('Failed to load popular videos:', error);
      }
    });
  }

  prevVideo(): void {
    console.log('prevVideo called, currentIndex before:', this.currentIndex);
    this.animating = true;
    setTimeout(() => {
      this.currentIndex = (this.currentIndex - 1 + this.popularVideos.length) % this.popularVideos.length;
      this.animating = false;
    }, 300);
    console.log('prevVideo called, currentIndex after:', this.currentIndex);
  }

  nextVideo(): void {
    console.log('nextVideo called, currentIndex before:', this.currentIndex);
    this.animating = true;
    setTimeout(() => {
      this.currentIndex = (this.currentIndex + 1) % this.popularVideos.length;
      this.animating = false;
    }, 300);
    console.log('nextVideo called, currentIndex after:', this.currentIndex);
  }

  goToVideos(): void {
    this.router.navigate(['/videos']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  goToUpload(): void {
    this.router.navigate(['/upload']);
  }

  goToMap(): void {
    this.router.navigate(['/map']);
  }

  goToVideo(videoId: string): void {
    this.router.navigate(['/videos', videoId]);
  }
}
