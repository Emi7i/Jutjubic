import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { VideoUploadComponent } from './components/video-upload/video-upload.component';
import { VideoListComponent } from './components/video-list/video-list.component';
import { VideoDetailComponent } from './components/video-detail/video-detail.component';

const routes: Routes = [
  { path: '', redirectTo: '/videos', pathMatch: 'full' },
  { path: 'videos', component: VideoListComponent },
  { path: 'videos/:id', component: VideoDetailComponent },
  { path: 'upload', component: VideoUploadComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
