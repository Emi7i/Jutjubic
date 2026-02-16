import { Component, OnInit, OnDestroy, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { PremiereService } from '../../services/premiere.service';
import { VideoService } from '../../services/video.service';

import {
  PremiereSession,
  PlaybackState,
  PremiereStatus,
  PremiereEvent,
  ChatMessageType,
  ChatMessage
} from '../../models/premiere';

@Component({
  selector: 'app-premiere-player',
  templateUrl: './premiere-player.component.html',
  styleUrls: ['./premiere-player.component.css']
})
export class PremierePlayerComponent implements OnInit, OnDestroy {
  @ViewChild('videoPlayer') videoPlayer!: ElementRef<HTMLVideoElement>;

  premiere: PremiereSession | null = null;
  playbackState: PlaybackState | null = null;
  premiereId: number = 0;
  videoUrl: string = '';

  // State tracking
  isConnected = false;
  isSyncing = false;
  loading = true;
  error: string | null = null;
  videoLoaded = false;
  isMuted = true; // Start muted to allow autoplay

  // Countdown
  countdown: string = '';
  countdownInterval?: any;

  // User tracking
  userId: string = '';


  chatMessages: ChatMessage[] = [];
  chatInput: string = '';
  chatEnabled: boolean = true;
  showChat: boolean = true;
  username: string = '';


  // Subscriptions
  private subscriptions: Subscription[] = [];
  private heartbeatInterval?: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private premiereService: PremiereService,
    private videoService: VideoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Get premiere ID from route
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Invalid premiere ID';
      return;
    }

    this.premiereId = parseInt(id, 10);
    this.userId = this.getUserId();

    // Load premiere data
    this.loadPremiere();

    // Connect to WebSocket
    this.connectWebSocket();

    this.username = this.getUsername();
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  private loadPremiere(): void {
    this.premiereService.getPremiere(this.premiereId).subscribe({
      next: (premiere) => {
        console.log('✅ Loaded premiere:', premiere);
        this.premiere = premiere;
        this.videoUrl = `${this.videoService['apiUrl']}/${premiere.videoId}/video`;
        this.loading = false;

        // Start countdown if scheduled
        if (premiere.status === PremiereStatus.SCHEDULED) {
          this.startCountdown();
        }
      },
      error: (error) => {
        console.error('❌ Error loading premiere:', error);
        this.error = 'Failed to load premiere';
        this.loading = false;
      }
    });
  }

  private connectWebSocket(): void {
    this.premiereService.connect();

    // Wait for connection
    const connSub = this.premiereService.isConnected().subscribe((connected) => {
      this.isConnected = connected;
      console.log('🔌 WebSocket connection status:', connected);

      if (connected) {
        // Subscribe to playback updates
        this.premiereService.subscribeToPlayback(this.premiereId);
        this.premiereService.subscribeToEvents(this.premiereId);
        // Join premiere
        this.premiereService.joinPremiere(this.premiereId, this.userId);
        // Subscribe to chat updates
        this.premiereService.subscribeToChat(this.premiereId);

        // Start heartbeat
        this.startHeartbeat();
      }
    });

    // Listen for playback state updates
    const stateSub = this.premiereService.getPlaybackStateUpdates().subscribe((state) => {
      console.log('📊 Received playback state update:', {
        playing: state.playing,
        position: state.currentPosition,
        status: state.status,
        viewers: state.viewerCount
      });

      this.playbackState = state;

      // Update premiere status from playback state
      if (this.premiere && state.status) {
        this.premiere.status = state.status;
        this.premiere.viewerCount = state.viewerCount;
      }

      this.syncVideo(state);
    });

    // Listen for premiere events
    const eventSub = this.premiereService.getPremiereEventUpdates().subscribe((event) => {
      console.log('📢 Received premiere event:', event.eventType);
      this.handlePremiereEvent(event);
    });

    // Listen for chat messages
    const chatSub = this.premiereService.getChatMessages().subscribe((message) => {
      console.log('💬 Received chat message:', message);
      this.chatMessages.push(message);

      // Auto-scroll to bottom
      setTimeout(() => this.scrollChatToBottom(), 100);

      // Limit messages in memory (keep last 100)
      if (this.chatMessages.length > 100) {
        this.chatMessages.shift();
      }
    });

    this.subscriptions.push(connSub, stateSub, eventSub, chatSub);
  }

  private syncVideo(state: PlaybackState): void {
    // Don't try to sync if video player doesn't exist yet
    if (!this.videoPlayer) {
      console.log('⏳ Video player not ready yet, skipping sync');
      return;
    }

    if (!this.videoLoaded) {
      console.log('⏳ Video not loaded yet, skipping sync');
      return;
    }

    if (this.isSyncing) {
      return;
    }

    const video = this.videoPlayer.nativeElement;
    this.isSyncing = true;

    // Calculate latency-adjusted position
    const adjustedPosition = this.premiereService.computeAdjustedPosition(state);

    // Sync position if drift > 0.5 seconds
    const drift = Math.abs(video.currentTime - adjustedPosition);
    if (drift > 0.5) {
      console.log(`🔄 Syncing video position: ${video.currentTime.toFixed(2)}s -> ${adjustedPosition.toFixed(2)}s (drift: ${drift.toFixed(2)}s)`);
      video.currentTime = adjustedPosition;
    }

    // Sync play/pause state
    if (state.playing && video.paused) {
      console.log('▶️ Starting video playback');
      this.playVideo();
    } else if (!state.playing && !video.paused) {
      console.log('⏸️ Pausing video playback');
      video.pause();
    }

    setTimeout(() => {
      this.isSyncing = false;
    }, 100);
  }

  private async playVideo(): Promise<void> {
    if (!this.videoPlayer) return;

    const video = this.videoPlayer.nativeElement;

    try {
      await video.play();
      console.log('✅ Video playing successfully');
    } catch (error: any) {
      console.error('❌ Video play failed:', error);

      // If autoplay was blocked, try muted playback
      if (error.name === 'NotAllowedError') {
        console.log('🔇 Autoplay blocked, trying muted playback...');
        this.isMuted = true;
        video.muted = true;

        try {
          await video.play();
          console.log('✅ Video playing muted');
          // Show unmute button to user
        } catch (mutedError) {
          console.error('❌ Even muted playback failed:', mutedError);
          this.error = 'Unable to play video. Please click to start.';
        }
      }
    }
  }

  private handlePremiereEvent(event: PremiereEvent): void {
    console.log('🎬 Handling premiere event:', event.eventType);

    switch (event.eventType) {
      case 'STARTED':
        console.log('▶️ Premiere STARTED event received');

        // Clear countdown
        if (this.countdownInterval) {
          clearInterval(this.countdownInterval);
          this.countdown = '';
        }

        // Update premiere status to trigger UI change
        if (this.premiere) {
          this.premiere.status = PremiereStatus.LIVE;
          console.log('✅ Updated premiere status to LIVE');
        }

        // If event includes updated data, use it
        if (event.data) {
          this.premiere = event.data;
          console.log('📝 Updated premiere from event data');
        }

        // Force Angular change detection
        this.cdr.detectChanges();

        // Give Angular a moment to render video element
        setTimeout(() => {
          if (this.videoPlayer) {
            console.log('📹 Video player rendered, ready for sync');
          } else {
            console.warn('⚠️ Video player still not available after status change');
          }
        }, 100);
        break;

      case 'PAUSED':
        console.log('⏸️ Premiere PAUSED event received');
        if (this.premiere) {
          this.premiere.status = PremiereStatus.PAUSED;
        }
        if (event.data) {
          this.premiere = event.data;
        }
        this.cdr.detectChanges();
        break;

      case 'RESUMED':
        console.log('▶️ Premiere RESUMED event received');
        if (this.premiere) {
          this.premiere.status = PremiereStatus.LIVE;
        }
        if (event.data) {
          this.premiere = event.data;
        }
        this.cdr.detectChanges();
        break;

      case 'FINISHED':
        console.log('🏁 Premiere FINISHED event received');
        if (this.premiere) {
          this.premiere.status = PremiereStatus.FINISHED;
        }
        if (event.data) {
          this.premiere = event.data;
        }

        // Stop the video
        if (this.videoPlayer) {
          this.videoPlayer.nativeElement.pause();
        }
        this.cdr.detectChanges();
        break;

      case 'CANCELLED':
        console.log('❌ Premiere CANCELLED event received');
        this.error = 'This premiere has been cancelled';
        if (this.premiere) {
          this.premiere.status = PremiereStatus.CANCELLED;
        }
        this.cdr.detectChanges();
        break;

      case 'UPDATED':
        console.log('📝 Premiere UPDATED event received');
        if (event.data) {
          const currentStatus = this.premiere?.status;
          this.premiere = event.data;
          if (!event.data.status && currentStatus) {
            this.premiere.status = currentStatus;
          }
        }
        this.cdr.detectChanges();
        break;
    }
  }

  // Video event handlers
  onVideoLoaded(): void {
    console.log('📹 Video metadata loaded');
    this.videoLoaded = true;

    // If we have a playback state and it says to play, start playing
    if(this.playbackState){
      this.syncVideo(this.playbackState)
    }
    if (this.playbackState?.playing) {
      console.log('🎬 Video loaded and premiere is playing, starting playback');
      this.playVideo();
    }
  }

  onVideoPlay(): void {
    console.log('▶️ Video play event');
  }

  onVideoPause(): void {
    console.log('⏸️ Video pause event');
  }

  onVideoError(event: any): void {
    console.error('❌ Video error:', event);
    const video = this.videoPlayer?.nativeElement;
    if (video && video.error) {
      console.error('Video error code:', video.error.code);
      console.error('Video error message:', video.error.message);
      this.error = `Video error: ${video.error.message || 'Unknown error'}`;
    }
  }

  toggleMute(): void {
    this.isMuted = !this.isMuted;
    if (this.videoPlayer) {
      this.videoPlayer.nativeElement.muted = this.isMuted;
      console.log('🔊 Video mute toggled:', this.isMuted);
    }
  }

  private startCountdown(): void {
    if (!this.premiere?.scheduledStartTime) return;

    console.log('⏰ Starting countdown for premiere scheduled at:', this.premiere.scheduledStartTime);

    this.countdownInterval = setInterval(() => {
      const now = new Date().getTime();
      const start = new Date(this.premiere!.scheduledStartTime + 'Z').getTime();
      const distance = start - now;

      if (distance < 0) {
        clearInterval(this.countdownInterval);
        this.countdown = 'Starting soon...';
        console.log('⏰ Countdown finished, waiting for premiere to start');
        return;
      }

      const days = Math.floor(distance / (1000 * 60 * 60 * 24));
      const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((distance % (1000 * 60)) / 1000);

      this.countdown = days > 0
        ? `${days}d ${hours}h ${minutes}m ${seconds}s`
        : `${hours}h ${minutes}m ${seconds}s`;
    }, 1000);
  }

  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      this.premiereService.sendHeartbeat(this.premiereId);
    }, 30000); // Every 30 seconds
  }

  private getUserId(): string {
    let userId = localStorage.getItem('userId');
    if (!userId) {
      userId = `user-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      localStorage.setItem('userId', userId);
    }
    return userId;
  }

  private cleanup(): void {
    console.log('🧹 Cleaning up premiere player');

    // Leave premiere
    if (this.isConnected) {
      this.premiereService.leavePremiere(this.premiereId, this.userId);
    }

    // Clear intervals
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }

    // Unsubscribe
    this.subscriptions.forEach(sub => sub.unsubscribe());

    // Disconnect WebSocket
    this.premiereService.disconnect();
  }

  goBack(): void {
    this.router.navigate(['/premieres']);
  }

  get PremiereStatus() {
    return PremiereStatus;
  }

  formatTime(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }

  private getUsername(): string {
    let username = localStorage.getItem('username');
    if (!username) {
      username = `Guest-${Math.random().toString(36).substr(2, 4)}`;
      localStorage.setItem('username', username);
    }
    return username;
  }

  sendMessage(): void {
    if (!this.chatInput || this.chatInput.trim().length === 0) {
      return;
    }

    if (this.chatInput.length > 500) {
      console.warn('Message too long');
      return;
    }

    console.log('💬 Sending chat message:', this.chatInput);

    this.premiereService.sendChatMessage(
      this.premiereId,
      this.userId,
      this.username,
      this.chatInput.trim()
    );

    this.chatInput = '';
  }

  toggleChat(): void {
    this.showChat = !this.showChat;
  }

  private scrollChatToBottom(): void {
    const chatContainer = document.querySelector('.chat-messages');
    if (chatContainer) {
      chatContainer.scrollTop = chatContainer.scrollHeight;
    }
  }

  getMessageTime(timestamp: number): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  get ChatMessageType() {
    return ChatMessageType;
  }
}
