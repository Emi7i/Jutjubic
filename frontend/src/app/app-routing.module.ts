import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { VideoUploadComponent } from './components/video-upload/video-upload.component';
import { VideoListComponent } from './components/video-list/video-list.component';
import { VideoDetailComponent } from './components/video-detail/video-detail.component';
import { RegisterComponent } from './pages/register/register.component';
import { LoginComponent } from "./pages/login/login.component";
import { ActivateComponent } from "./pages/activate/activate.component";
import { SandboxComponent } from "./pages/sandbox/sandbox.component";
import { VideoMapComponent } from "./pages/video-map/video-map.component";


const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'videos', component: VideoListComponent },
  { path: 'videos/:id', component: VideoDetailComponent },
  { path: 'upload', component: VideoUploadComponent },
  { path: 'map', component: VideoMapComponent },
  // authorization stuff: registration, login, etc.
  { path: 'register', component: RegisterComponent },
  { path: 'login', component: LoginComponent },
  { path: 'activate', component: ActivateComponent },
  // sandbox
  { path: 'sandbox', component: SandboxComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
