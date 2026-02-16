// video-map.component.ts
import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { MapComponent } from '../../components/map/map.component';
import { VideoService } from '../../services/video.service';
import { Video, Comment } from '../../models/video-upload';
import { DateInterval } from  '../../shared/timeframe-selector/timeframe-selector.component';

@Component({
  selector: 'app-video-map',
  templateUrl: './video-map.component.html',
  styleUrls: ['./video-map.component.css']
})
export class VideoMapComponent implements AfterViewInit {

  @ViewChild(MapComponent) mapComp!: MapComponent;

  zoom = 13;
  visibleVideos: Video[] = [];
  selectedVideo?: Video;
  videoOpen = false;
  filtersOpen = false;
  currentInterval: DateInterval = { from: null, to: null };

  comments: Comment[] = [];
  newCommentText: string = '';

  constructor(private videoService: VideoService) {}

  ngAfterViewInit(): void {
    this.zoom = this.mapComp.getZoom();
  }

  zoomIn(): void {
    this.mapComp.zoomIn();
    this.zoom = this.mapComp.getZoom();
  }

  zoomOut(): void {
    this.mapComp.zoomOut();
    this.zoom = this.mapComp.getZoom();
  }

  darkMode(): void {
    this.mapComp.setDarkMode(!this.mapComp.darkMode);
  }

  onSliderChange(event: any): void {
    const newZoom = Number(event.target.value);
    this.mapComp.setZoom(newZoom);
    this.zoom = newZoom;
  }

  onMapZoomChange(newZoom: number): void {
    this.zoom = newZoom;
  }

  onVideoSelected(video: Video) {
    // If a video is already selected, close it first
    if (this.videoOpen) {
      this.videoOpen = false;
      this.selectedVideo = undefined;
      this.comments = [];
    }

    // Slight delay ensures Angular refreshes the template
    setTimeout(() => {
      this.selectedVideo = { ...video }; // copy to ensure change detection
      this.videoOpen = true;

      if (video.location) {
        this.mapComp.focusLocation(
          video.location.latitude,
          video.location.longitude
        );
      }

      this.loadComments(video.id);
    }, 0);
  }



  closeVideoCard(): void {
    this.videoOpen = false;
    this.mapComp.closeAnyPopup();
    this.selectedVideo = undefined;
    this.comments = [];
  }

  toggleFilters(): void {
    this.filtersOpen = !this.filtersOpen;
  }

  onIntervalChange(interval: DateInterval) {
    this.currentInterval = interval;
    this.refreshVisibleVideos();
  }

  onMapMoved(bounds: { sw: [number, number], ne: [number, number] }) {
    this.refreshVisibleVideos(bounds.sw, bounds.ne);
  }

  private refreshVisibleVideos(sw?: [number, number], ne?: [number, number]) {
    if (!sw || !ne) {
      const bounds = this.mapComp.getVisibleArea();
      sw = bounds.sw;
      ne = bounds.ne;
    }

    this.videoService.getVideosInBoundingBoxTuples(
      sw, ne,
      this.currentInterval.from ?? undefined,
      this.currentInterval.to ?? undefined
    ).subscribe(videos => this.visibleVideos = videos);
  }

  // --- Comments ---
  loadComments(videoId: string) {
    this.videoService.getComments(videoId).subscribe(comments => {
      this.comments = comments;
    });
  }

  addComment() {
    if (!this.selectedVideo || !this.newCommentText.trim()) return;

    const text = this.newCommentText.trim();
    this.videoService.addComment(this.selectedVideo.id, text).subscribe(comment => {
      this.comments.push(comment);
      this.selectedVideo!.commentsCount += 1;
      this.newCommentText = '';
    });
  }

  // --- Likes ---
  toggleLike(video: Video) {
    // Check if user is logged in
    const currentUser = localStorage.getItem('userId');
    if (!currentUser) {
      alert('You need to log in to like or comment on videos.');
      return;
    }

    this.videoService.toggleLike(video.id, video.isLiked).subscribe(() => {
      video.isLiked = !video.isLiked;
      video.likes += video.isLiked ? 1 : -1;
    });
  }
}
