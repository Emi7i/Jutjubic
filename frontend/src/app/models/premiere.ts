// Premiere-related models

export interface PremiereSession {
  id: number;
  videoId: number;
  videoTitle: string;
  status: PremiereStatus;
  scheduledStartTime: Date;
  actualStartTime?: Date;
  endedAt?: Date;
  currentPosition: number;
  playing: boolean;
  viewerCount: number;
  allowReplay: boolean;
  chatEnabled: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export enum PremiereStatus {
  SCHEDULED = 'SCHEDULED',
  LIVE = 'LIVE',
  PAUSED = 'PAUSED',
  FINISHED = 'FINISHED',
  CANCELLED = 'CANCELLED'
}

export interface PlaybackState {
  premiereId: number;
  playing: boolean;
  currentPosition: number;
  lastStateChangeEpoch: number;
  serverTimestamp: number;
  status: PremiereStatus;
  viewerCount: number;
}

export interface PremiereEvent {
  eventType: PremiereEventType;
  premiereId: number;
  timestamp: number;
  data?: PremiereSession;
  message?: string;
}

export enum PremiereEventType {
  CREATED = 'CREATED',
  UPDATED = 'UPDATED',
  STARTED = 'STARTED',
  PAUSED = 'PAUSED',
  RESUMED = 'RESUMED',
  SEEKED = 'SEEKED',
  FINISHED = 'FINISHED',
  CANCELLED = 'CANCELLED',
  VIEWER_JOINED = 'VIEWER_JOINED',
  VIEWER_LEFT = 'VIEWER_LEFT'
}

export interface CreatePremiereRequest {
  videoId: number;
  scheduledStartTime: Date;
  allowReplay?: boolean;
  chatEnabled?: boolean;
}

export interface ChatMessage {
  premiereId: number;
  userId: string;
  username: string;
  message: string;
  timestamp: number;
  type: ChatMessageType;
}

export enum ChatMessageType {
  USER = 'USER',
  SYSTEM = 'SYSTEM',
  ADMIN = 'ADMIN'
}
