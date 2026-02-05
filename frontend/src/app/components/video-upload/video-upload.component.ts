import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VideoService } from '../../services/video.service';
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
  
  // File references for upload
  thumbnailFile: File | null = null;
  videoFile: File | null = null;

  constructor(private fb: FormBuilder, private videoService: VideoService) {}

  ngOnInit(): void {
    this.uploadForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
    });
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

    // Call the actual upload service
    this.videoService.uploadVideo(videoData).subscribe({
      next: (progress) => {
        this.uploadProgress = {
          message: progress.message || 'Uploading...',
          percentage: progress.percentage
        };
        
        if (progress.status === 'complete') {
          this.isUploading = false;
          this.showSuccessMessage = true;
          // Auto-hide success message after 5 seconds
          setTimeout(() => {
            this.hideSuccessMessage();
          }, 5000);
          this.resetForm();
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
      // Store the file reference for upload
      this.thumbnailFile = file;
      
      // Generate preview
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
      // Validate that the file is a video
      if (!file.type.startsWith('video/')) {
        this.errorMessage = 'Please select a valid video file. Images are not allowed.';
        event.target.value = ''; // Clear the input
        return;
      }
      
      // Check for specific video formats
      const allowedTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime', 'video/x-msvideo'];
      if (!allowedTypes.includes(file.type)) {
        this.errorMessage = 'Unsupported video format. Please use MP4, WebM, OGG, QuickTime, or AVI files.';
        event.target.value = ''; // Clear the input
        return;
      }
      
      // Store the file reference for upload
      this.videoFile = file;
      
      this.errorMessage = null; // Clear any previous errors
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
  }

  hideSuccessMessage(): void {
    this.showSuccessMessage = false;
  }
}
