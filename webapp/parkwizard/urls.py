"""
    Link parkwizard views to url endpoints
"""
from django.conf.urls import url

from . import views

urlpatterns = [
    url(r'^addparking$', views.addparking, name='addparking'),
    url(r'^searchparking$', views.searchparking, name='searchparking'),
    url(r'^adduser$', views.adduser, name='adduser'),
    url(r'^getscore$', views.getscore, name='getscore'),
    url(r'^getupdatelocations$', views.getupdatelocations,
        name='getupdatelocations'),
    url(r'^updateparking$', views.updateparking, name='updateparking'),
]
