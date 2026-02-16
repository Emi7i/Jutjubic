import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VideoService } from '../../services/video.service';
import { PremiereService } from '../../services/premiere.service';
import { VideoUpload } from '../../models/video-upload';

@Component({
  selector: 'app-video-upload',
  templateUrl: './video-upload.component.html',
  styleUrls: ['./video-upload.component.css']
})
export class VideoUploadComponent implements OnInit {
  uploadForm!: FormGroup;
  isUploading = false;
  tags: string[] = [];
  tagInput = '';
  thumbnailPreview: string | null = null;
  videoPreview: string | null = null;
  useLocation = false;
  currentLocation: any = null;
  errorMessage: string | null = null;
  uploadProgress: { message: string; percentage: number } | null = null;
  showSuccessMessage = false;

  // Premiere-specific fields
  isPremiere = false;
  scheduledStartTime: string = '';
  allowReplay = false;
  chatEnabled = true;
  minDateTime: string = '';

  // File references for upload
  thumbnailFile: File | null = null;
  videoFile: File | null = null;
  uploadedVideoId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private videoService: VideoService,
    private premiereService: PremiereService
  ) {}

  ngOnInit(): void {
    this.uploadForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
    });

    // Set minimum datetime to now
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    this.minDateTime = now.toISOString().slice(0, 16);
  }

  onSubmit(): void {
    if (this.uploadForm.invalid || this.isUploading) {
      return;
    }

    // Validate that both files are selected
    if (!this.thumbnailFile || !this.videoFile) {
      this.errorMessage = 'Please select both thumbnail and video files.';
      return;
    }

    // Validate premiere fields if enabled
    if (this.isPremiere && !this.scheduledStartTime) {
      this.errorMessage = 'Please select a start time for the premiere.';
      return;
    }

    this.isUploading = true;
    this.errorMessage = null;

    // Prepare video upload data
    const videoData: VideoUpload = {
      title: this.uploadForm.value.title,
      description: this.uploadForm.value.description,
      tags: this.tags,
      thumbnail: this.thumbnailFile,
      video: this.videoFile,
      location: this.useLocation ? this.currentLocation : null
    };

    // Upload the video first
    this.videoService.uploadVideo(videoData).subscribe({
      next: (progress) => {
        this.uploadProgress = {
          message: progress.message || 'Uploading...',
          percentage: progress.percentage
        };

        if (progress.status === 'complete') {
          // Get the uploaded video ID from localStorage
          const videoIdStr = localStorage.getItem('lastUploadedVideoId');
          this.uploadedVideoId = videoIdStr ? parseInt(videoIdStr, 10) : null;

          // If premiere is enabled, create premiere session
          if (this.isPremiere) {
            this.createPremiereSession();
          } else {
            this.finishUpload();
          }
        }
      },
      error: (error) => {
        console.error('Upload error:', error);
        this.errorMessage = 'Failed to upload video: ' + (error.error?.message || error.message || 'Unknown error');
        this.isUploading = false;
        this.uploadProgress = null;
      }
    });
  }

  createPremiereSession(): void {
    if (!this.uploadedVideoId) {
      this.errorMessage = 'Video uploaded but video ID not found. Cannot create premiere. Please try uploading again.';
      this.isUploading = false;
      this.uploadProgress = null;
      // Clean up localStorage
      localStorage.removeItem('lastUploadedVideoId');
      return;
    }

    const premiereRequest = {
      videoId: this.uploadedVideoId,
      scheduledStartTime: new Date(this.scheduledStartTime),
      allowReplay: this.allowReplay,
      chatEnabled: this.chatEnabled
    };

    this.premiereService.createPremiere(premiereRequest).subscribe({
      next: (premiere) => {
        console.log('Premiere created:', premiere);
        localStorage.removeItem('lastUploadedVideoId'); // Clean up
        this.finishUpload();
      },
      error: (error) => {
        console.error('Premiere creation error:', error);
        this.errorMessage = 'Video uploaded but failed to create premiere: ' + (error.error?.message || error.message);
        this.isUploading = false;
        this.uploadProgress = null;
        localStorage.removeItem('lastUploadedVideoId'); // Clean up
      }
    });
  }

  finishUpload(): void {
    this.isUploading = false;
    this.showSuccessMessage = true;
    // Auto-hide success message after 5 seconds
    setTimeout(() => {
      this.hideSuccessMessage();
    }, 5000);
    this.resetForm();
  }

  addTag(): void {
    const tag = this.tagInput.trim();
    if (tag && !this.tags.includes(tag)) {
      this.tags.push(tag);
      this.tagInput = '';
    }
  }

  removeTag(tag: string): void {
    this.tags = this.tags.filter(t => t !== tag);
  }

  onThumbnailSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.thumbnailFile = file;

      const reader = new FileReader();
      reader.onload = (e) => {
        this.thumbnailPreview = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  onVideoSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (!file.type.startsWith('video/')) {
        this.errorMessage = 'Please select a valid video file. Images are not allowed.';
        event.target.value = '';
        return;
      }

      const allowedTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime', 'video/x-msvideo'];
      if (!allowedTypes.includes(file.type)) {
        this.errorMessage = 'Unsupported video format. Please use MP4, WebM, OGG, QuickTime, or AVI files.';
        event.target.value = '';
        return;
      }

      this.videoFile = file;
      this.errorMessage = null;

      const reader = new FileReader();
      reader.onload = (e) => {
        this.videoPreview = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  toggleLocation(): void {
    if (this.useLocation) {
      this.getCurrentLocation();
    } else {
      this.currentLocation = null;
    }
  }

  getCurrentLocation(): void {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.currentLocation = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          };
        },
        (error) => {
          this.errorMessage = 'Unable to get location: ' + error.message;
          this.useLocation = false;
        }
      );
    } else {
      this.errorMessage = 'Geolocation is not supported by this browser';
      this.useLocation = false;
    }
  }

  togglePremiere(): void {
    if (this.isPremiere && !this.scheduledStartTime) {
      // Set default to 1 hour from now
      const future = new Date();
      future.setHours(future.getHours() + 1);
      future.setMinutes(future.getMinutes() - future.getTimezoneOffset());
      this.scheduledStartTime = future.toISOString().slice(0, 16);
    }
  }

  resetForm(): void {
    this.uploadForm.reset();
    this.tags = [];
    this.tagInput = '';
    this.thumbnailPreview = null;
    this.videoPreview = null;
    this.thumbnailFile = null;
    this.videoFile = null;
    this.useLocation = false;
    this.currentLocation = null;
    this.errorMessage = null;
    this.uploadProgress = null;
    this.isPremiere = false;
    this.scheduledStartTime = '';
    this.allowReplay = false;
    this.chatEnabled = true;
    this.uploadedVideoId = null;
    // Clean up localStorage
    localStorage.removeItem('lastUploadedVideoId');
  }

  hideSuccessMessage(): void {
    this.showSuccessMessage = false;
  }
}
