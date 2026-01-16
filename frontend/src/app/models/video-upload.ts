export interface VideoUpload {
  title: string;
  description: string;
  tags: string[];
  thumbnail: File;
  video: File;
  location?: GeographicLocation;
}

export interface GeographicLocation {
  latitude: number;
  longitude: number;
  address?: string;
}

export interface Video {
  id: string;
  title: string;
  description: string;
  tags: string[];
  thumbnailUrl: string;
  videoUrl: string;
  location?: GeographicLocation;
  createdAt: Date;
  userId: string;
  userName: string;
  likes: number;
  commentsCount: number;
  viewsCount: number;
  isLiked?: boolean;
}

export interface Comment {
  id: string;
  videoId: string;
  userId: string;
  userName: string;
  text: string;
  createdAt: Date;
}

export interface UploadProgress {
  percentage: number;
  status: 'pending' | 'uploading' | 'processing' | 'complete' | 'error';
  message?: string;
}
