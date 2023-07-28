#!/bin/bash
VM_IP=20.254.38.125
scp -r /home/daniel/workspace/thesis/concurrent_abtree/out/production/concurrent_abtree adminuser@${VM_IP}:/home/adminuser/occab
