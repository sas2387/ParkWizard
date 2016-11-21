"""
    Link parkwizard views to url endpoints
"""
from django.conf.urls import url

from . import views

urlpatterns = [
    url(r'^addparking$', views.addparking, name='addparking'),
    url(r'^getparking$', views.getparking, name='getparking'),
]
