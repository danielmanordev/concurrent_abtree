#!/bin/bash
scp -r /home/daniel/workspace/thesis/concurrent_abtree/out/production/concurrent_abtree adminuser@${VM_IP}:/home/adminuser/occab
scp -r test_script.sh adminuser@${VM_IP}:/home/adminuser/occab