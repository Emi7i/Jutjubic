import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PremiereService } from '../../services/premiere.service';
import { PremiereSession, PremiereStatus } from '../../models/premiere';

@Component({
  selector: 'app-premiere-list',
  templateUrl: './premiere-list.component.html',
  styleUrls: ['./premiere-list.component.css']
})
export class PremiereListComponent implements OnInit {
  livePremieres: PremiereSession[] = [];
  upcomingPremieres: PremiereSession[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private premiereService: PremiereService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPremieres();

    // Refresh every 30 seconds
    setInterval(() => {
      this.loadPremieres();
    }, 30000);
  }

  loadPremieres(): void {
    // Load live premieres
    this.premiereService.getLivePremieres().subscribe({
      next: (premieres) => {
        this.livePremieres = premieres;
      },
      error: (error) => {
        console.error('Error loading live premieres:', error);
      }
    });

    // Load upcoming premieres
    this.premiereService.getUpcomingPremieres().subscribe({
      next: (premieres) => {
        this.upcomingPremieres = premieres;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading upcoming premieres:', error);
        this.error = 'Failed to load premieres';
        this.loading = false;
      }
    });
  }

  watchPremiere(premiereId: number): void {
    this.router.navigate(['/premiere', premiereId]);
  }

  getTimeUntil(scheduledTime: Date): string {
    const now = new Date().getTime();
    const start = new Date(scheduledTime).getTime();
    const distance = start - now;

    if (distance < 0) return 'Starting soon';

    const days = Math.floor(distance / (1000 * 60 * 60 * 24));
    const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));

    if (days > 0) return `in ${days}d ${hours}h`;
    if (hours > 0) return `in ${hours}h ${minutes}m`;
    return `in ${minutes}m`;
  }

  getThumbnailUrl(videoId: number): string {
    return `http://localhost:8080/api/video-posts/${videoId}/thumbnail`;
  }

  get PremiereStatus() {
    return PremiereStatus;
  }
}
