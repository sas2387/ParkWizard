from django.conf.urls import url

from . import views

urlpatterns = [
    url(r'^addparking$', views.addparking, name='addparking'),
]