import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { MapComponent } from '../../components/map/map.component';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video-upload';

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['./sandbox.component.css']
})
export class SandboxComponent implements AfterViewInit {

  @ViewChild(MapComponent) mapComp!: MapComponent;

  zoom = 13; // initial zoom
  visibleVideos: Video[] = [];
  selectedVideo?: Video;
  videoOpen = false;
  filtersOpen = false;


  constructor(private videoService: VideoService) {} // inject the service

  ngAfterViewInit(): void {
    // sync initial zoom from map
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

  onMapMoved(bounds: { sw: [number, number], ne: [number, number] }) {
    // Fetch videos in the visible bounds
    if(!this.selectedVideo)
      this.visibleVideos = this.videoService.getVideosInBounds(bounds);
  }

  onVideoSelected(video: Video) {
    this.selectedVideo = video;
    this.videoOpen = true;

    if (video.location) {
      this.mapComp.focusLocation(
        video.location.latitude,
        video.location.longitude
      );
    }
  }

  onPopupClosed(): void {
    this.videoOpen = false;
  }

  closeVideoCard(): void {
    this.videoOpen = false;
    this.mapComp.closeAnyPopup();
  }


  toggleFilters(): void {
    this.filtersOpen = !this.filtersOpen;
  }
}
