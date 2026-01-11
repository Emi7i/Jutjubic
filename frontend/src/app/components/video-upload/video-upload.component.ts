import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

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

  constructor(private fb: FormBuilder) {}

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

    this.isUploading = true;
    this.errorMessage = null;
    
    // TODO: Implement actual upload logic
    console.log('Form submitted:', {
      ...this.uploadForm.value,
      tags: this.tags,
      thumbnail: this.thumbnailPreview,
      video: this.videoPreview,
      location: this.useLocation ? this.currentLocation : null
    });

    // Simulate upload progress
    this.uploadProgress = { message: 'Uploading...', percentage: 0 };
    const interval = setInterval(() => {
      if (this.uploadProgress && this.uploadProgress.percentage < 100) {
        this.uploadProgress.percentage += 10;
      } else {
        clearInterval(interval);
        this.isUploading = false;
        this.uploadProgress = null;
        this.resetForm();
      }
    }, 500);
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
    this.useLocation = false;
    this.currentLocation = null;
    this.errorMessage = null;
    this.uploadProgress = null;
  }
}
