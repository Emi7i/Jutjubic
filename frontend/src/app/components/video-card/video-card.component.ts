import { Component, Input, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { Video, Comment } from '../../models/video-upload';
import { InteractionService } from '../../services/interaction.service';

@Component({
  selector: 'app-video-card',
  templateUrl: './video-card.component.html',
  styleUrls: ['./video-card.component.css']
})
export class VideoCardComponent implements OnInit, AfterViewInit {

  @Input() video!: Video;

  comments: Comment[] = [];
  newComment = '';
  loadingComments = false;
  errorMessage = '';

  @ViewChild('videoElement') videoRef!: ElementRef<HTMLVideoElement>;

  constructor(private interactionService: InteractionService) { }

  ngOnInit(): void {
    this.loadComments();
  }

  ngAfterViewInit(): void {
    // Optionally, autoplay or setup video element here
  }

  /** Likes */
  toggleLike(): void {
    if (!this.video) return;

    this.interactionService.toggleLike(this.video.id).subscribe({
      next: updatedVideo => {
        this.video.isLiked = updatedVideo.isLiked;
        this.video.likes = updatedVideo.likes;
      },
      error: err => alert(err.message)
    });
  }

  /** Comments */
  loadComments(): void {
    this.loadingComments = true;
    this.interactionService.loadComments(this.video.id).subscribe({
      next: comments => {
        this.comments = comments;
        this.loadingComments = false;
      },
      error: err => {
        console.error(err);
        this.loadingComments = false;
      }
    });
  }

  addComment(): void {
    if (!this.newComment.trim()) return;

    this.interactionService.addComment(this.video.id, this.newComment).subscribe({
      next: comment => {
        this.comments.unshift(comment);
        this.newComment = '';
        this.video.commentsCount++;
      },
      error: err => {
        console.error(err);
        alert(err.message);
      }
    });
  }

  /** Video controls */
  play(): void {
    this.videoRef.nativeElement.play();
  }

  pause(): void {
    this.videoRef.nativeElement.pause();
  }

  togglePlayPause(): void {
    const videoEl = this.videoRef.nativeElement;
    if (videoEl.paused) videoEl.play();
    else videoEl.pause();
  }
}
